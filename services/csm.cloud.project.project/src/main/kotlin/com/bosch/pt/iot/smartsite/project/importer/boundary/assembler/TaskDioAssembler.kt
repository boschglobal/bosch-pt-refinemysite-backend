/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.CraftDioAssemblerUtils.getCraft
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.ProjectUtils.isP6Xml
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.WorkAreaDioAssemblerUtils.getWorkArea
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ResourcesColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.TaskFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.TaskDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import net.sf.mpxj.ActivityStatus
import net.sf.mpxj.Notes
import net.sf.mpxj.ParentNotes
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.StructuredNotes
import net.sf.mpxj.Task
import net.sf.mpxj.TaskField
import org.springframework.stereotype.Component

@Component
class TaskDioAssembler : DioAssembler() {

  fun assemble(
      projectFile: ProjectFile,
      context: ImportContext,
      craftColumn: ImportColumn?,
      crafts: List<CraftDio>,
      readWorkAreasRecursive: Boolean,
      workAreaColumn: ImportColumn?
  ): List<TaskDio> {
    val skipIds =
        if (readWorkAreasRecursive)
            context.map.keys.filterIsInstance<WorkAreaIdentifier>().map { it.id }
        else emptyList()

    val craftIdentifiersByName = crafts.associate { requireNotNull(it.lookUpName) to it.id }

    val isP6Xml = isP6Xml(projectFile)

    return projectFile.tasks
        .asSequence()
        .filter {
          // The project itself is the task with no parent.
          // Tasks in P6 XML files do not have parents.
          isP6Xml || it.parentTask != null
        }
        .filter { !it.hasChildTasks() } // Filter intermediate levels (tasks that have children)
        .filter { // Filter milestones and tasks without schedules
          val start = it.actualStart ?: it.start
          val finish = it.actualFinish ?: it.finish
          !(start == finish && start != null || it.milestone)
        }
        .filter { !skipIds.contains(it.uniqueID) } // Filter work areas
        .map { task ->
          val craftId =
              getCraft(task, craftColumn, craftIdentifiersByName)
                  ?: throw IllegalStateException("Craft expected")
          val workAreaId = getWorkArea(task, workAreaColumn, context, readWorkAreasRecursive)

          val isCraftResourcesColumn = isResourceColumn(craftColumn)
          val isWorkAreaResourceColumn = isResourceColumn(workAreaColumn)

          val assignedCraftNamesFromAssignedResources =
              if (isCraftResourcesColumn) task.resourceAssignments.mapNotNull { it.resource?.name }
              else emptyList()

          val assignedWorkAreaNamesFromAssignedResources =
              if (isWorkAreaResourceColumn)
                  task.resourceAssignments.mapNotNull { it.resource?.name }
              else emptyList()

          val status = getStatus(task)

          TaskDio(
              TaskIdentifier(task.uniqueID),
              task.guid,
              task.uniqueID,
              task.id,
              task.activityID,
              task.name,
              task.notesObject?.let { getNotes(it) },
              craftId,
              workAreaId,
              status,
              assignedCraftNamesFromAssignedResources,
              assignedWorkAreaNamesFromAssignedResources)
        }
        .toList()
  }

  private fun getNotes(notes: Notes): String? =
      when (notes) {
        is ParentNotes ->
            notes.childNotes
                ?.map { getNotes(it) }
                ?.filter { it != "" }
                ?.let { if (it.size > 1) it.joinToString("\n") else it.joinToString("") }
                ?.trim()
        is StructuredNotes ->
            // Skip the topic name for now as we don't want to see the prefix "Notes: "
            // in the output
            getNotes(notes.notes)
        // The export or import seem to add additional whitespaces to the description.
        // They are removed and the string is re-concatenated.
        else -> notes.toString().split("\n").joinToString("\n") { it.trim() }
      }

  private fun getStatus(task: Task) =
      task.activityStatus?.let {
        when (it) {
          ActivityStatus.NOT_STARTED -> TaskStatusEnum.DRAFT
          ActivityStatus.IN_PROGRESS -> TaskStatusEnum.STARTED
          ActivityStatus.COMPLETED -> TaskStatusEnum.ACCEPTED
        }
      }
          ?: task.percentageComplete?.let {
            when (it.toInt()) {
              0 -> TaskStatusEnum.DRAFT
              in 1..99 -> TaskStatusEnum.STARTED
              100 -> TaskStatusEnum.ACCEPTED
              else -> TaskStatusEnum.DRAFT
            }
          }
              ?: TaskStatusEnum.DRAFT

  private fun isResourceColumn(importColumn: ImportColumn?): Boolean =
      importColumn?.let { column ->
        column is ResourcesColumn ||
            column is TaskFieldColumn && column.fieldType == TaskField.RESOURCE_NAMES
      }
          ?: false
}
