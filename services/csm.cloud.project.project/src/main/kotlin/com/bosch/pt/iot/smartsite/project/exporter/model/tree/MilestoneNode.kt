/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import com.bosch.pt.iot.smartsite.project.exporter.api.MilestoneExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import java.time.LocalDate
import net.sf.mpxj.ActivityType.FINISH_MILESTONE
import net.sf.mpxj.Duration
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import net.sf.mpxj.TaskMode.AUTO_SCHEDULED
import net.sf.mpxj.TaskMode.MANUALLY_SCHEDULED
import net.sf.mpxj.TimeUnit.DAYS
import org.springframework.context.MessageSource

class MilestoneNode(
    override val identifier: MilestoneId,
    val milestone: Milestone,
    override val externalId: ExternalId?,
    private val requestedSchedulingType: MilestoneExportSchedulingType,
    override val children: MutableList<AbstractNode> = mutableListOf()
) : AbstractNode() {

  override val startDate: LocalDate = milestone.date
  override val finishDate: LocalDate = milestone.date

  override fun write(
      projectFile: ProjectFile,
      parent: Task?,
      messageSource: MessageSource
  ): List<ExternalId> {
    checkNotNull(externalId)

    addMilestone(projectFile, parent)
    return listOf(externalId)
  }

  private fun addMilestone(projectFile: ProjectFile, parent: Task?) =
      addTask(projectFile, parent).also {
        checkNotNull(externalId)

        // Update the external ID with the inserted id
        externalId.fileId = it.id

        // Set functional parameters
        setFunctionalParameters(it)

        // Set all structure related IDs.
        setStructureRelatedIds(it)
      }

  private fun setFunctionalParameters(task: Task) {
    task.activityType = FINISH_MILESTONE
    task.milestone = true
    task.name = milestone.name
    task.notes = milestone.description

    if (requestedSchedulingType == MilestoneExportSchedulingType.MANUALLY_SCHEDULED) {
      task.actualStart = milestone.date.atStartOfDay()
      task.actualFinish = milestone.date.atStartOfDay()
      task.taskMode = MANUALLY_SCHEDULED
    } else {
      task.plannedStart = milestone.date.atStartOfDay()
      task.remainingEarlyStart = milestone.date.atStartOfDay()
      task.plannedFinish = milestone.date.atStartOfDay()
      task.remainingEarlyFinish = milestone.date.atStartOfDay()
      task.taskMode = AUTO_SCHEDULED
    }

    task.duration = Duration.getInstance(0, DAYS)

    milestone.craft?.also { task.setFieldByAlias(FIELD_ALIAS_CRAFT, it.name) }
  }

  private fun setStructureRelatedIds(it: Task) {
    checkNotNull(externalId)

    it.guid = externalId.guid
    it.uniqueID = externalId.fileUniqueId
    it.id = externalId.fileId
    it.activityID = externalId.activityId
  }

  override fun toString(): String =
      "MilestoneNode(identifier=$identifier," +
          "name=${milestone.name}, " +
          "parent-identifier=${milestone.workArea?.identifier}, " +
          "parent-name=${milestone.workArea?.name})"
}
