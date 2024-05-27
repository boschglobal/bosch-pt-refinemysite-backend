/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary

import com.bosch.pt.iot.smartsite.project.daycard.command.service.precondition.DayCardPrecondition
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isReschedulablePossible
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import com.bosch.pt.iot.smartsite.project.task.shared.dto.TaskRescheduleResult
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch.UpdateTaskScheduleBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.query.TaskScheduleQueryService
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import datadog.trace.api.Trace
import java.time.LocalDate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskRescheduleService(
    private val taskQueryService: TaskQueryService,
    private val taskScheduleQueryService: TaskScheduleQueryService,
    private val updateTaskScheduleBatchCommandHandler: UpdateTaskScheduleBatchCommandHandler
) {

  @Trace
  @PreAuthorize(
      "@taskAuthorizationComponent.hasReschedulePermissionOnProject(#criteria.projectIdentifier)")
  @Transactional(readOnly = true)
  open fun validate(criteria: SearchTasksDto): TaskRescheduleResult {
    val affectedTasks = findAffectedTasks(criteria)
    val (successfulTasks, failedTasks) = affectedTasks.partition { it.isReschedulable() }

    val affectedSchedules = findAffectedSchedules(successfulTasks)
    val (successfulSchedules, failedSchedules) =
        affectedSchedules.partition { it.isReschedulable() }

    return taskRescheduleValidationResult(
        Pair(successfulTasks, failedTasks), Pair(successfulSchedules, failedSchedules))
  }

  @Trace
  @PreAuthorize(
      "@taskAuthorizationComponent.hasReschedulePermissionOnProject(#criteria.projectIdentifier)")
  @Transactional
  open fun reschedule(shiftDays: Long, criteria: SearchTasksDto): TaskRescheduleResult {
    val affectedTasks = findAffectedTasks(criteria)
    val (successfulTasks, failedTasks) = affectedTasks.partition { it.isReschedulable() }

    val affectedSchedules = findAffectedSchedules(successfulTasks)
    val (successfulSchedules, failedSchedules) =
        affectedSchedules.partition { it.isReschedulable() }

    if (successfulSchedules.isNotEmpty()) {
      updateSchedules(successfulSchedules, shiftDays)
    }

    return taskRescheduleValidationResult(
        Pair(successfulTasks, failedTasks), Pair(successfulSchedules, failedSchedules))
  }

  /** filter to remove tasks that cannot be seen in the calendar */
  private fun findAffectedTasks(criteria: SearchTasksDto) =
      taskQueryService.findTasksWithDetailsForFilters(criteria).content.filterNot {
        it.isVisibleInCalendar()
      }

  private fun findAffectedSchedules(successfulTasks: List<Task>) =
      taskScheduleQueryService.findByTaskIdentifiers(successfulTasks.map { it.identifier })

  private fun taskRescheduleValidationResult(
      taskValidationResult: Pair<List<Task>, List<Task>>,
      scheduleValidationResult:
          Pair<List<TaskScheduleWithDayCardsDto>, List<TaskScheduleWithDayCardsDto>>
  ): TaskRescheduleResult {
    val successful = scheduleValidationResult.first.map { it.taskIdentifier }
    val failed =
        taskValidationResult.second
            .map { it.identifier }
            .plus(scheduleValidationResult.second.map { it.taskIdentifier })

    return TaskRescheduleResult(successful, failed)
  }
  private fun updateSchedules(
      successfulSchedules: List<TaskScheduleWithDayCardsDto>,
      shiftDays: Long
  ) {
    val updateTaskScheduleCommands =
        successfulSchedules.map {
          UpdateTaskScheduleCommand(
              identifier = it.identifier,
              taskIdentifier = it.taskIdentifier,
              version = it.version,
              start = it.start!!.plusDays(shiftDays),
              end = it.end!!.plusDays(shiftDays),
              slots = it.scheduleSlotsWithDayCards.toMap().valuesPlusDays(shiftDays))
        }

    updateTaskScheduleBatchCommandHandler.handle(updateTaskScheduleCommands)
  }

  /** converts to a map of day card identifier -> date */
  private fun List<TaskScheduleSlotWithDayCardDto>.toMap(): Map<DayCardId, LocalDate> =
      this.associateBy(
          keySelector = { it.slotsDayCardIdentifier }, valueTransform = { it.slotsDate })

  private fun Map<DayCardId, LocalDate>.valuesPlusDays(days: Long) =
      this.mapValues { (_, value) -> value.plusDays(days) }

  /** predicates used based on update pre-condition of task and day card */
  private fun Task.isReschedulable() = isReschedulablePossible(this.status)

  private fun Task.isVisibleInCalendar() =
      this.taskSchedule == null ||
          (this.taskSchedule!!.start == null && this.taskSchedule!!.end == null) ||
          (this.taskSchedule!!.start != null && this.taskSchedule!!.end == null) ||
          (this.taskSchedule!!.start == null && this.taskSchedule!!.end != null)

  private fun TaskScheduleWithDayCardsDto.isReschedulable() =
      this.scheduleSlotsWithDayCards.all {
        DayCardPrecondition.isReschedulePossible(it.slotsDayCardStatus)
      }
}
