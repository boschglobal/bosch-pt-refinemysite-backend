/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_DAY_CARD_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_SLOTS_NOT_MATCH
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_MULTIPLE_DAY_CARD_AT_SAME_POSITION
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshot
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import com.google.common.collect.Comparators
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class TaskScheduleCommandHandlerHelper(
    private val projectRepository: ProjectRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val dayCardRepository: DayCardRepository,
    private val taskRepository: TaskRepository
) {

  fun findTaskScheduleOrFail(
      taskIdentifier: TaskId,
  ): TaskSchedule =
      taskScheduleRepository.findOneByTaskIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  fun findTaskOrFail(command: CreateTaskScheduleCommand) =
      (taskRepository.findOneByIdentifier(command.taskIdentifier)
          ?: throw AggregateNotFoundException(
              Key.TASK_VALIDATION_ERROR_NOT_FOUND, command.taskIdentifier.toString()))

  fun updateSlotOrder(slots: MutableList<TaskScheduleSlotDto>) {
    if (slots != null &&
        !Comparators.isInOrder(
            slots,
            Comparator.comparingLong { slot: TaskScheduleSlotDto -> slot.date.toEpochDay() })) {

      slots.sortWith(
          Comparator.comparingLong { slot: TaskScheduleSlotDto -> slot.date.toEpochDay() })
    }
  }

  fun getChangedSlots(
      schedule: TaskScheduleSnapshot,
      newSlotDates: Map<DayCardId, LocalDate>?
  ): MutableMap<DayCardId, LocalDate> {
    val changedSlots: MutableMap<DayCardId, LocalDate> = HashMap()
    for (slot in schedule.slots!!) {
      val slotIdentifier = slot.id
      if (!slot.date.isEqual(requireNotNull(newSlotDates)[slotIdentifier])) {
        changedSlots[slotIdentifier] = newSlotDates[slotIdentifier]!!
      }
    }
    return changedSlots
  }

  fun validateTaskSchedule(
      startDate: LocalDate?,
      endDate: LocalDate?,
      slots: List<TaskScheduleSlotDto>
  ) {
    assertScheduleRangeIsValid(startDate, endDate, slots)
    assertAllDayCardsWithinScheduleRange(startDate, endDate, slots)
    assertNoSlotIsOccupiedTwice(slots)
  }

  fun validateDayCards(newSlotDates: Map<DayCardId, LocalDate>?, schedule: TaskScheduleSnapshot) {
    assertSlotsDoNotContainNullValues(newSlotDates)
    assertNoDayCardIsMissingInNewSlots(schedule, newSlotDates)
    assertNoDayCardInNonOpenStatusWasMoved(schedule, newSlotDates)
  }

  fun assertNoDayCardInNonOpenStatusWasMoved(
      schedule: TaskScheduleSnapshot,
      newSlotDates: Map<DayCardId, LocalDate>?
  ) {
    for (slot in schedule.slots!!) {
      val dayCard = dayCardRepository.findEntityByIdentifier(slot.id)
      if (dayCard!!.status != DayCardStatusEnum.OPEN &&
          slot.date != requireNotNull(newSlotDates)[slot.id]) {
        throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
      }
    }
  }

  fun assertNoDayCardIsMissingInNewSlots(
      schedule: TaskScheduleSnapshot,
      newSlotDates: Map<DayCardId, LocalDate>?
  ) {
    if (schedule.slots!!.size != newSlotDates?.size) {
      throw PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_SLOTS_NOT_MATCH)
    }

    for (slot in schedule.slots) {
      if (!newSlotDates.containsKey(slot.id)) {
        throw PreconditionViolationException(
            TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_DAY_CARD_NOT_FOUND)
      }
    }
  }

  fun assertSlotsDoNotContainNullValues(slots: Map<DayCardId, LocalDate>?) =
      require(
          slots != null &&
              !(slots.containsKey(null as DayCardId?) || slots.containsValue(null as LocalDate?))) {
            "Slot contains a null key or value."
          }

  fun assertTaskSchedulesFromSameProject(taskSchedules: Collection<TaskSchedule>) {
    if (taskSchedules
        .map { taskSchedule: TaskSchedule -> taskSchedule.task.project.identifier }
        .distinct()
        .count() != 1) {
      throw PreconditionViolationException(Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)
    }
  }

  fun assertTasksFromSameProject(tasks: Collection<Task>) {
    if (tasks.map { task: Task -> task.project.identifier }.distinct().count() != 1) {
      throw PreconditionViolationException(Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)
    }
  }

  fun assertScheduleRangeIsValid(
      start: LocalDate?,
      end: LocalDate?,
      slots: List<TaskScheduleSlotDto>
  ) {
    if (start == null && end == null ||
        start != null && end != null && start.isAfter(end) ||
        (start == null || end == null) && slots.isNotEmpty()) {
      throw PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)
    }
  }

  fun findProjectOrFail(projectIdentifier: ProjectId): Project =
      projectRepository.findOneByIdentifier(projectIdentifier)
          ?: throw PreconditionViolationException(COMMON_VALIDATION_ERROR_PROJECT_NOT_FOUND)

  fun findByTaskIdentifierOrFail(taskIdentifier: TaskId): TaskSchedule =
      taskScheduleRepository.findOneByTaskIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  private fun assertNoSlotIsOccupiedTwice(slots: List<TaskScheduleSlotDto>) =
      slots.forEach { assertTaskScheduleSlotIsNotOccupiedTwice(slots, it.date) }

  private fun assertAllDayCardsWithinScheduleRange(
      startDate: LocalDate?,
      endDate: LocalDate?,
      slots: List<TaskScheduleSlotDto>
  ) = slots.forEach { assertDayCardDate(it.date, startDate!!, endDate!!) }

  private fun assertDayCardDate(
      dayCardDate: LocalDate,
      scheduleStartDate: LocalDate,
      scheduleEndDate: LocalDate
  ) {
    if (dayCardDate.isBefore(scheduleStartDate) || dayCardDate.isAfter(scheduleEndDate)) {
      throw PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE)
    }
  }

  private fun assertTaskScheduleSlotIsNotOccupiedTwice(
      slots: List<TaskScheduleSlotDto>,
      date: LocalDate
  ) {
    if (slots.count { slot: TaskScheduleSlotDto -> slot.date == date } > 1) {
      throw PreconditionViolationException(
          TASK_SCHEDULE_VALIDATION_ERROR_MULTIPLE_DAY_CARD_AT_SAME_POSITION)
    }
  }
}
