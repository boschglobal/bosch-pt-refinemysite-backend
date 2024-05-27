/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType.MS_PROJECT
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS as ChronoUnitDays
import java.util.regex.Pattern
import net.sf.mpxj.ActivityType.WBS_SUMMARY
import net.sf.mpxj.Duration
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_AFTERNOON
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_MORNING
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import net.sf.mpxj.TimeUnit.DAYS as TimeUnitDays
import org.springframework.context.MessageSource

class WorkAreaNode(
    override val identifier: WorkAreaId,
    val workArea: WorkArea?,
    override val externalId: ExternalId?,
    override val children: MutableList<AbstractNode> = mutableListOf()
) : AbstractNode() {

  override var startDate: LocalDate? = null

  override var finishDate: LocalDate? = null

  override fun write(
      projectFile: ProjectFile,
      parent: Task?,
      messageSource: MessageSource
  ): List<ExternalId> {
    checkNotNull(externalId)

    val mpxjTask = addWorkArea(projectFile, parent)
    return listOf(externalId) +
        children
            .sortNodes()
            .map { it.write(projectFile, mpxjTask, messageSource) }
            .flatten()
            .also {

              // Recalculate start/end date after inserting children
              startDate =
                  children
                      .filter { it.startDate != null }
                      .map { it.startDate }
                      .minOfOrNull { checkNotNull(it) }
              finishDate =
                  children
                      .filter { it.finishDate != null }
                      .map { it.finishDate }
                      .maxOfOrNull { checkNotNull(it) }

              // Set function parameters
              setFunctionalParameters(mpxjTask)
            }
  }

  private fun addWorkArea(projectFile: ProjectFile, parent: Task?): Task =
      addTask(projectFile, parent).also {
        checkNotNull(externalId)

        // Update the external ID with the inserted id
        externalId.fileId = it.id

        // Set all structure related IDs.
        setStructureRelatedIds(it)
      }

  private fun setFunctionalParameters(task: Task) {
    task.activityType = WBS_SUMMARY
    task.name = workArea?.name

    // Set dates
    if (checkNotNull(externalId).idType == MS_PROJECT) {
      startDate?.atStartOfDay()?.plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())?.let {
        task.start = it
      }
      finishDate?.atStartOfDay()?.plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())?.let {
        task.finish = it
      }

      // Set duration
      if (startDate != null && finishDate != null) {
        val days = ChronoUnitDays.between(startDate, finishDate)
        task.duration = Duration.getInstance(days.toInt(), TimeUnitDays)
      }
    }
  }

  private fun setStructureRelatedIds(it: Task) {
    checkNotNull(externalId)

    it.guid = externalId.guid
    it.uniqueID = externalId.fileUniqueId
    it.activityID = externalId.activityId
    if (externalId.wbs != null) {
      val matcher = wbsPrefixPattern.matcher(externalId.wbs!!)
      if (matcher.find()) {
        val prefix = matcher.group(1)
        it.wbs = externalId.wbs!!.substring(prefix.length)
      } else {
        it.wbs = externalId.wbs
      }
    }
  }

  override fun toString(): String =
      "WorkAreaNode(identifier=$identifier, " +
          "name=${workArea?.name}, " +
          "parent=${workArea?.parent}, " +
          "children=${children.count()})"

  companion object {
    val wbsPrefixPattern: Pattern = Pattern.compile("([^0-9.]+)[0-9].*")
  }
}
