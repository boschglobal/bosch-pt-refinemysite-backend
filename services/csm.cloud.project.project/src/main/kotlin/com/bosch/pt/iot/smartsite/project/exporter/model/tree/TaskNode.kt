/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import com.bosch.pt.iot.smartsite.project.task.shared.model.Task as RmsTask
import net.sf.mpxj.Duration as MpxjDuration
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType.AUTO_SCHEDULED
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType.MANUALLY_SCHEDULED
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.Description
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.TaskDescription
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType.MS_PROJECT
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType.P6
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import net.sf.mpxj.ActivityType.TASK_DEPENDENT
import net.sf.mpxj.Notes
import net.sf.mpxj.ParentNotes
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_AFTERNOON
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_MORNING
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import net.sf.mpxj.TaskMode
import net.sf.mpxj.TimeUnit
import org.springframework.context.MessageSource

class TaskNode(
    override val identifier: TaskId,
    val rmsTask: RmsTask,
    override val externalId: ExternalId?,
    private val allowWorkOnNonWorkingDays: Boolean,
    private val requestedSchedulingType: TaskExportSchedulingType,
    private val notes: List<Description>,
    private val dayCards: List<DayCard>,
    override val children: MutableList<AbstractNode> = mutableListOf()
) : AbstractNode() {

  override val startDate: LocalDate? = rmsTask.taskSchedule?.start
  override val finishDate: LocalDate? = rmsTask.taskSchedule?.end

  override fun write(
      projectFile: ProjectFile,
      parent: Task?,
      messageSource: MessageSource
  ): List<ExternalId> {
    checkNotNull(externalId)

    addRmsTask(projectFile, parent, messageSource)
    return listOf(externalId)
  }

  private fun addRmsTask(
      projectFile: ProjectFile,
      parent: Task?,
      messageSource: MessageSource
  ): Task =
      addTask(projectFile, parent).also {
        checkNotNull(externalId)

        // Update external ID with the inserted id
        externalId.fileId = it.id

        // Set functional parameters
        it.setFunctionalParameters(projectFile, notes, messageSource)

        // Set all structure related IDs
        it.setStructureRelatedIds()
      }

  private inline fun <T, R> T.forMsProject(block: T.() -> R) {
    if (externalId?.idType == MS_PROJECT) {
      block()
    }
  }

  private inline fun <T, R> T.forPrimaveraP6(block: T.() -> R) {
    if (externalId?.idType == P6) {
      block()
    }
  }

  override fun toString(): String =
      "TaskNode(identifier=$identifier, " +
          "name=${rmsTask.name}, " +
          "parent-identifier=${rmsTask.workArea?.identifier}, " +
          "parent-name=${rmsTask.workArea?.name})"

  private fun Task.setFunctionalParameters(
      projectFile: ProjectFile,
      notes: List<Description>,
      messageSource: MessageSource,
  ) {
    checkNotNull(externalId)

    this.activityType = TASK_DEPENDENT
    this.name = rmsTask.name

    // Set notes instead of nodesObject as the format is slightly different
    // (additional whitespaces in P6)
    val exportNotes: MutableList<Description> =
        rmsTask.description?.let { mutableListOf(TaskDescription(it, LocalDateTime.MIN)) }
            ?: mutableListOf()

    exportNotes.addAll(notes)

    this.notes =
        if (exportNotes.isNotEmpty())
            ParentNotes(exportNotes.map { Notes(it.toDisplayValue(messageSource)) }).toString()
        else null

    // Prepare parameters to detect scheduling mode
    val taskProperties =
        TaskProperties(
            isNotStarted = rmsTask.status in notStartedStatus,
            isInProgress = rmsTask.status in inProgressStatus,
            isFinished = rmsTask.status in finishedStatus,
            startDateInThePast = startDate?.isBefore(LocalDate.now()) == true,
            startOnNonWorkday = startDate?.let { !projectFile.defaultCalendar.isWorkingDate(it) }
                    ?: false,
            finishOnNonWorkday = finishDate?.let { !projectFile.defaultCalendar.isWorkingDate(it) }
                    ?: false,
        )

    this.setPercentageComplete(taskProperties)
    val calculatedTaskProperties = taskProperties.calculate(projectFile)

    this.setTaskModeForMsProject(taskProperties)
    this.setSchedulingPropertiesForMsProject(calculatedTaskProperties)

    this.setSchedulingPropertiesForPrimaveraP6(
        calculatedTaskProperties, taskProperties, requestedSchedulingType)

    // Set created date
    this.createDate = rmsTask.createdDate.orElse(null)

    // The task status cannot be set manually as it is only exported into P6 files.
    // Additionally, independently what's set as status it is automatically overwritten
    // by the library - by analyzing the actualStart and actualFinish date.
    // Therefore - for now - we decided to not export the task status.

    rmsTask.projectCraft.let { this.setFieldByAlias(FIELD_ALIAS_CRAFT, it.name) }
  }

  private fun TaskProperties.shouldPreventReschedulingFromNonWorkingDays() =
      this.startOrFinishOnNonWorkingDay && (this.isFinished || this.isInProgress)

  /**
   * Sets [TaskMode] specifically for MS Project exports as [TaskMode] is ignored in Primavera P6
   * exports. (See [net.sf.mpxj.primavera.PrimaveraPMProjectWriter] for details). In certain cases
   * manual scheduling is chosen over automatic scheduling to ensure compatibility with RmS
   * scheduling (i.e. on re-import).
   */
  private fun Task.setTaskModeForMsProject(taskProperties: TaskProperties) = forMsProject {
    // Special case which sets MANUAL scheduling mode to prevent rescheduling from non-working days
    if (taskProperties.shouldPreventReschedulingFromNonWorkingDays()) {
      this.taskMode = TaskMode.MANUALLY_SCHEDULED
    } else if (requestedSchedulingType == MANUALLY_SCHEDULED) {
      this.taskMode = TaskMode.MANUALLY_SCHEDULED
    } else {
      this.taskMode = TaskMode.AUTO_SCHEDULED
    }
  }

  /**
   * MS Project does not handle planned and actual values well. Therefore [Task.setStart],
   * [Task.setFinish] and [Task.setDuration] are used relying on [Task.setPercentageComplete] and
   * [Task.setTaskMode] to handle the details.
   */
  private fun Task.setSchedulingPropertiesForMsProject(
      calculatedTaskProperties: CalculatedTaskProperties
  ) = forMsProject {
    calculatedTaskProperties.let {
      this.start = it.start
      this.finish = it.finish
      this.duration = it.duration
    }
  }

  private fun Task.setSchedulingPropertiesForPrimaveraP6(
      calculatedTaskProperties: CalculatedTaskProperties,
      taskProperties: TaskProperties,
      requestedSchedulingType: TaskExportSchedulingType
  ) = forPrimaveraP6 {
    calculatedTaskProperties.let {
      fun setActualStart() {
        this.actualStart = it.start
        this.remainingEarlyStart = it.start
      }

      fun setActualFinish() {
        this.actualFinish = it.finish
        // unset
        this.remainingEarlyStart = null
        this.remainingEarlyFinish = null
      }

      fun setPlannedStart() {
        this.remainingEarlyStart = it.start
        this.plannedStart = it.start
      }

      fun setPlannedFinish() {
        this.remainingEarlyFinish = it.finish
        this.plannedFinish = it.finish
      }

      fun isFinishedByStatusOrClosedDayCards(): Boolean {
        val dayCardStatus = dayCards.groupBy { dayCard -> dayCard.status }
        return taskProperties.isFinished || DayCardStatusEnum.OPEN in dayCardStatus
      }

      // Set start and finish
      if (requestedSchedulingType == AUTO_SCHEDULED) {
        if (taskProperties.isNotStarted) setPlannedStart() else setActualStart()
        if (isFinishedByStatusOrClosedDayCards()) setActualFinish() else setPlannedFinish()
      } else if (requestedSchedulingType == MANUALLY_SCHEDULED) {
        setActualStart()
        setActualFinish()
      }

      // Set duration
      if (this.actualStart != null && this.actualFinish != null) {
        this.actualDuration = it.duration
      } else {
        this.duration = it.duration
      }
    }
  }

  private fun Task.setPercentageComplete(taskProperties: TaskProperties) {
    // Set percentage complete
    if (taskProperties.isFinished) this.percentageComplete = HUNDRED_PERCENT
    else if (taskProperties.isInProgress) this.percentageComplete = FIFTY_PERCENT
    else if (taskProperties.isNotStarted) this.percentageComplete = ZERO_PERCENT
  }

  private fun Task.setStructureRelatedIds() {
    checkNotNull(externalId)

    this.guid = externalId.guid
    this.uniqueID = externalId.fileUniqueId
    this.activityID = externalId.activityId
    if (this.wbs == null) {
      this.generateWBS(this.parentTask)
    }
    if (this.activityID == null) {
      this.activityID = this.wbs
    }
  }

  private fun ProjectFile.getStartDate(date: LocalDate): LocalDate =
      with(this.defaultCalendar.getStartTime(date)) {
        this?.let { date } ?: getStartDate(date.plusDays(1))
      }

  private fun ProjectFile.getFinishDate(date: LocalDate): LocalDate =
      with(this.defaultCalendar.getFinishTime(date)) {
        this?.let { date } ?: getFinishDate(date.plusDays(1))
      }

  data class CalculatedTaskProperties(
      val start: LocalDateTime?,
      val finish: LocalDateTime?,
      val duration: net.sf.mpxj.Duration?
  )

  data class TaskProperties(
      val isNotStarted: Boolean,
      val isFinished: Boolean,
      val isInProgress: Boolean,
      val startDateInThePast: Boolean,
      val startOnNonWorkday: Boolean,
      val finishOnNonWorkday: Boolean,
      val startOrFinishOnNonWorkingDay: Boolean = startOnNonWorkday || finishOnNonWorkday
  )

  private fun TaskProperties.calculate(projectFile: ProjectFile): CalculatedTaskProperties {

    val start: LocalDateTime?
    val finish: LocalDateTime?
    if (allowWorkOnNonWorkingDays ||
        (startDateInThePast && startOrFinishOnNonWorkingDay && (isInProgress || isFinished))) {
      start = startDate?.atStartOfDay()?.plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())
      finish = finishDate?.atStartOfDay()?.plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())
    } else {
      start =
          startDate?.atStartOfDay()?.let {
            val date = it.toLocalDate()
            val startHour = DEFAULT_WORKING_MORNING.start.hour.toLong()
            projectFile.getStartDate(date).atStartOfDay().plusHours(startHour)
          }
      finish =
          finishDate?.atStartOfDay()?.let {
            val shift = start?.toLocalDate()?.until(startDate)?.days ?: 0
            val shiftedDate = it.plusDays(shift.toLong()).toLocalDate()
            val endHour = DEFAULT_WORKING_AFTERNOON.end.hour.toLong()
            projectFile.getFinishDate(shiftedDate).atStartOfDay().plusHours(endHour)
          }
    }

    fun getDuration(): MpxjDuration? =
        if (start != null && finish != null) {
          val firstDayIsWorkday = projectFile.defaultCalendar.isWorkingDate(start.toLocalDate())
          val lastDayIsWorkday = projectFile.defaultCalendar.isWorkingDate(finish.toLocalDate())
          val duration = projectFile.defaultCalendar.getDuration(start, finish)
          if (firstDayIsWorkday && lastDayIsWorkday) {
            duration
          } else {
            // Calculate 1 additional day for each start or finish on non-working day
            val additionalDays =
                when {
                  !firstDayIsWorkday && !lastDayIsWorkday -> 2.0
                  else -> 1.0
                }

            // Add one additional day for the end-day of the task (equivalent to MS Project)
            MpxjDuration.getInstance(
                duration.duration.toDuration(DurationUnit.DAYS).toLong(DurationUnit.DAYS) +
                    additionalDays,
                TimeUnit.DAYS)
          }
        } else {
          null
        }

    return CalculatedTaskProperties(start, finish, getDuration())
  }

  companion object {
    const val HUNDRED_PERCENT = 100
    const val FIFTY_PERCENT = 50
    const val ZERO_PERCENT = 0
    val notStartedStatus = listOf(DRAFT, OPEN)
    val finishedStatus = listOf(CLOSED, ACCEPTED)
    val inProgressStatus = listOf(STARTED)
  }
}
