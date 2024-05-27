/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.CraftDioAssemblerUtils.getCraftIdentifierFromRow
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.ProjectUtils.isP6Xml
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.WorkAreaDioAssemblerUtils.getWorkArea
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.MilestoneDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.MilestoneIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskScheduleIdentifier
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import org.springframework.stereotype.Component

@Component
class MilestoneDioAssembler : DioAssembler() {

  fun assemble(
      projectFile: ProjectFile,
      projectId: ProjectIdentifier,
      context: ImportContext,
      craftColumn: ImportColumn?,
      crafts: List<CraftDio>,
      readWorkAreasRecursive: Boolean,
      workAreaColumn: ImportColumn?
  ): List<MilestoneDio> {
    val skipIds =
        context.map.keys
            .filter { it is TaskIdentifier || it is TaskScheduleIdentifier }
            .map { it.id }

    val craftIdentifiersByName = crafts.associate { requireNotNull(it.lookUpName) to it.id }

    val isP6Xml = isP6Xml(projectFile)

    return projectFile.tasks
        .asSequence()
        .filter { isMilestone(it) }
        .filter { !skipIds.contains(it.uniqueID) }
        .filter {
          // The project itself is the task with no parent
          // Tasks in P6 XML files do not have parents.
          isP6Xml || it.parentTask != null
        }
        .filter { !it.hasChildTasks() } // Filter intermediate levels (tasks that have children)
        .mapNotNull { task ->
          val craftId = getCraftIdentifierFromRow(task, craftColumn, craftIdentifiersByName)
          val workAreaId = getWorkArea(task, workAreaColumn, context, readWorkAreasRecursive)

          val milestoneType =
              if (craftId == null) MilestoneTypeEnum.PROJECT else MilestoneTypeEnum.CRAFT
          val header = workAreaId == null

          // In P6 there are different milestone types that we are currently not supporting:
          // - Finishing Milestone
          // - Starting Milestone
          // Therefore we check all dates and choose the one that is set
          fun getDateIfSet() =
              task.actualFinish
                  ?: task.plannedFinish ?: task.finish ?: task.actualStart ?: task.plannedStart
                      ?: task.start

          // Filter out milestones without date (can at least happen in powerproject)
          val date = getDateIfSet() ?: return@mapNotNull null

          MilestoneDio(
              MilestoneIdentifier(task.uniqueID),
              task.guid,
              task.uniqueID,
              task.id,
              task.activityID,
              task.name,
              milestoneType,
              date.toLocalDate(),
              header,
              projectId,
              task.notes?.trim(),
              craftId,
              workAreaId)
        }
        .toList()
  }

  private fun isMilestone(it: Task): Boolean {
    val start = it.actualStart ?: it.plannedStart ?: it.start
    val finish = it.actualFinish ?: it.plannedFinish ?: it.finish
    return start == finish && start != null || it.milestone
  }
}
