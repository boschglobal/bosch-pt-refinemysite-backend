/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.model.dio.WorkAreaDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import net.sf.mpxj.TaskContainer
import net.sf.mpxj.TaskField.NAME
import org.springframework.stereotype.Component

@Component
class WorkAreaDioAssembler : DioAssembler() {

  fun assemble(
      projectFile: ProjectFile,
      context: ImportContext,
      projectId: ProjectIdentifier,
      workAreaColumn: ImportColumn?,
      readRecursive: Boolean,
      placeholderWorkAreaCount: AtomicInteger
  ): List<WorkAreaDio> {
    val tasks = projectFile.tasks

    val optionalRootNode =
        tasks.firstOrNull {
          it.parentTask == null &&
              (it.name == projectFile.projectProperties.projectTitle ||
                  it.name == projectFile.projectProperties.name)
        }

    // TODO block delete if parent set - delete only if children wbs are deleted
    return if (readRecursive) {

      // Read working areas hierarchically
      readWorkAreasFromWbs(context, optionalRootNode, tasks, projectId, placeholderWorkAreaCount)
    } else {

      readWorkAreasFromTableColumn(
          context, optionalRootNode, workAreaColumn, tasks, projectId, placeholderWorkAreaCount)
    }
  }

  private fun readWorkAreasFromTableColumn(
      context: ImportContext,
      optionalRootNode: Task?,
      workAreaColumn: ImportColumn?,
      tasks: TaskContainer,
      projectId: ProjectIdentifier,
      placeholderWorkAreaCount: AtomicInteger
  ): List<WorkAreaDio> =
      workAreaColumn
          ?.let { column ->
            tasks
                .filter { isNotRootNode(optionalRootNode, it) }
                .map {
                  // Take name either from column or from parent if column is wbs/wbs-name
                  val name = ColumnUtils.getColumnValue(it, column)
                  PotentialWorkArea(it, name?.trim(), optionalRootNode != null)
                }
                .filter { it.isWorkArea() }
                .map { asWorkAreaDio(context, it, projectId, placeholderWorkAreaCount) }
          }
          ?.toSet() // Deduplicate entries
          ?.sortedBy { it.name } // Sort them alphabetically
       ?: listOf()

  private fun readWorkAreasFromWbs(
      context: ImportContext,
      optionalRootNode: Task?,
      tasks: TaskContainer,
      projectId: ProjectIdentifier,
      placeholderWorkAreaCount: AtomicInteger
  ): List<WorkAreaDio> =
      tasks
          .asSequence()
          .filter { it.hasChildTasks() }
          .filter { isNotRootNode(optionalRootNode, it) }
          .map {
            val name = it.get(NAME)?.toString()
            PotentialWorkArea(it, name?.trim(), optionalRootNode != null)
          }
          .filter { it.isWorkArea() }
          .map {
            val workAreaDio = asWorkAreaDio(context, it, projectId, placeholderWorkAreaCount)
            context.map[workAreaDio.id] = workAreaDio.identifier
            context.dioMap[workAreaDio.id] = workAreaDio
            workAreaDio
          }
          .sortedBy { it.name } // Sort them alphabetically
          .toList()

  private fun asWorkAreaDio(
      context: ImportContext,
      it: PotentialWorkArea,
      projectId: ProjectIdentifier,
      placeholderWorkAreaCount: AtomicInteger
  ) =
      WorkAreaDio(
          id = WorkAreaIdentifier(it.uniqueId),
          guid = it.guid,
          uniqueId = it.uniqueId,
          fileId = it.id,
          activityId = it.activityId,
          wbs = it.wbs,
          name = it.name,
          parent = workAreaIdOfParentTask(context, it.task),
          projectId = projectId,
          placeholderWorkAreaCount = placeholderWorkAreaCount)

  private fun isNotRootNode(optionalRootNode: Task?, it: Task) =
      optionalRootNode != null && it.id != optionalRootNode.id || optionalRootNode == null

  companion object {

    fun workAreaIdOfParentTask(context: ImportContext, task: Task): UUID? =
        task.parentTask?.uniqueID?.let { uniqueId ->
          context
              .get(uniqueId, WorkAreaIdentifier::class.java)
              ?.let { context.getDio(it) as WorkAreaDio? }
              ?.identifier
        }

    fun workAreaIdByName(context: ImportContext, workAreaName: String): WorkAreaIdentifier? =
        context.dioMap.values
            .firstOrNull { it is WorkAreaDio && it.name == workAreaName }
            ?.let { it as WorkAreaDio }
            ?.id
  }

  class PotentialWorkArea(val task: Task, val name: String?, private val fileHasRootNode: Boolean) {

    fun isWorkArea(): Boolean = !isHiddenRootNode()

    private fun isHiddenRootNode() = fileHasRootNode && task.parentTask == null

    val id: Int
      get() = task.id

    val uniqueId: Int
      get() = task.uniqueID

    val guid: UUID?
      get() = task.guid

    val activityId: String?
      get() = task.activityID

    val wbs: String?
      get() =
          task.let {
            val projectId = it.parentFile.projectProperties.projectID
            val shortened =
                task.wbs?.let {
                  if (projectId != null && it.contains(projectId)) {
                    it.substring(projectId.length)
                  } else {
                    it
                  }
                }
            if (shortened?.startsWith(".") == true) {
              shortened.substring(1)
            } else {
              shortened
            }
          }
  }
}
