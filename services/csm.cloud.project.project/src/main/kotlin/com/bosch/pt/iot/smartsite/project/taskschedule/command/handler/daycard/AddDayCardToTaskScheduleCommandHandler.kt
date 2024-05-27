/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.daycard

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_DATE
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.ScheduleSlotHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.AddDayCardsToTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshot
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshotStore
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import java.time.LocalDate
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AddDayCardToTaskScheduleCommandHandler(
    private val snapshotStore: TaskScheduleSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val workdayConfigurationRepository: WorkdayConfigurationRepository,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskAuthorizationComponent.hasContributePermissionOnTask(#command.taskIdentifier)")
  open fun handle(command: AddDayCardsToTaskScheduleCommand) {
    val taskScheduleSnapshot = snapshotStore.findOrFail(command.taskScheduleIdentifier)

    command.eTag.verify(taskScheduleSnapshot.version)

    assertDayCardDateIsWithinScheduleRange(command.date, taskScheduleSnapshot)

    val workdayConfiguration =
        findWorkDayConfigurationByProjectIdentifier(command.projectIdentifier)
    assertDayCardDateIsValidWorkingDay(command.date, workdayConfiguration)

    // Check if task schedule slot is occupied. If true, move slots by one day to free the slot
    val slots = taskScheduleSnapshot.slots!!.toMutableList()
    if (slots.any { it.date == command.date }) {
      updateSlots(
          command.taskIdentifier,
          taskScheduleSnapshot,
          ScheduleSlotHelper.shiftDayCardOutFromSlot(
              command.date, slots.toIdentifierToDateMap(), workdayConfiguration))
    }

    // Add new day card to task schedule slots
    slots.add(TaskScheduleSlotDto(command.dayCardIdentifier, command.date))

    taskScheduleCommandHandlerHelper.validateTaskSchedule(
        taskScheduleSnapshot.start,
        taskScheduleSnapshot.end,
        taskScheduleSnapshot.slots.map { TaskScheduleSlotDto(it.id, it.date) })

    taskScheduleCommandHandlerHelper.updateSlotOrder(slots)

    snapshotStore
        .findOrFail(taskScheduleSnapshot.identifier)
        .toCommandHandler()
        .update { it.copy(slots = slots.map { TaskScheduleSlotDto(it.id, it.date) }) }
        .emitEvent(TaskScheduleEventEnumAvro.UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun findWorkDayConfigurationByProjectIdentifier(
      projectIdentifier: ProjectId
  ): WorkdayConfiguration =
      requireNotNull(
          workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier))

  private fun assertDayCardDateIsValidWorkingDay(
      date: LocalDate,
      workdayConfiguration: WorkdayConfiguration
  ) =
      with(workdayConfiguration) {
        val holidays = holidays.map { it.date }.toSet()

        if (!allowWorkOnNonWorkingDays &&
            (!workingDays.contains(date.dayOfWeek) || holidays.contains(date))) {
          throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_DATE)
        }
      }

  private fun assertDayCardDateIsWithinScheduleRange(
      date: LocalDate,
      schedule: TaskScheduleSnapshot
  ) {
    if (schedule.start == null || schedule.end == null) {
      throw PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)
    } else if (date.isBefore(LocalDate.from(schedule.start)) || date.isAfter(schedule.end)) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_DATE_TIMES)
    }
  }

  private fun updateSlots(
      taskIdentifier: TaskId,
      schedule: TaskScheduleSnapshot,
      newSlotDates: Map<DayCardId, LocalDate>?
  ) {
    taskScheduleCommandHandlerHelper.validateDayCards(newSlotDates, schedule)

    val changedSlots: MutableMap<DayCardId, LocalDate> =
        taskScheduleCommandHandlerHelper.getChangedSlots(schedule, newSlotDates)

    if (changedSlots.isNotEmpty()) {
      if (taskAuthorizationComponent.hasContributePermissionOnTask(taskIdentifier)) {
        for (slot in schedule.slots!!) {
          slot.date = changedSlots.getOrDefault(slot.id, slot.date)
        }
      } else {
        throw AccessDeniedException("User not permitted to edit day card dates")
      }
    }
  }

  private fun List<TaskScheduleSlotDto>.toIdentifierToDateMap() =
      this.associate { it.id to it.date }.toMutableMap()
}
