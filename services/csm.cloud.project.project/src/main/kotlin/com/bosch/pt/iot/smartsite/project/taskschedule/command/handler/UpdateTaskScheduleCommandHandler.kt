/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshot
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshotStore
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import datadog.trace.api.Trace
import java.time.LocalDate
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateTaskScheduleCommandHandler(
    private val snapshotStore: TaskScheduleSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize(usedByController = true)
  open fun handle(command: UpdateTaskScheduleCommand) {
    val oldSnapshot = snapshotStore.findOrFail(command.identifier)
    ETag.from(command.version!!).verify(oldSnapshot.version)

    taskScheduleCommandHandlerHelper.assertScheduleRangeIsValid(
        command.start, command.end, oldSnapshot.slots!!.map { TaskScheduleSlotDto(it.id, it.date) })

    val startDate = updateScheduleStartDate(command.taskIdentifier, oldSnapshot, command.start)
    val endDate = updateScheduleEndDate(command.taskIdentifier, oldSnapshot, command.end)

    val taskScheduleSlotDtos = updateSlots(command.taskIdentifier, oldSnapshot, command.slots)

    taskScheduleCommandHandlerHelper.validateTaskSchedule(startDate, endDate, taskScheduleSlotDtos)

    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .update {
          it.copy(
              start = startDate,
              end = endDate,
              taskIdentifier = command.taskIdentifier,
              slots = taskScheduleSlotDtos)
        }
        .emitEvent(TaskScheduleEventEnumAvro.UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun updateScheduleStartDate(
      taskIdentifier: TaskId,
      schedule: TaskScheduleSnapshot,
      startDate: LocalDate?,
  ): LocalDate? {
    return if (schedule.start != startDate) {
      if (taskAuthorizationComponent.hasEditPermissionOnTask(taskIdentifier)) {
        startDate
      } else {
        throw AccessDeniedException("User not permitted to edit task dates")
      }
    } else {
      schedule.start
    }
  }

  private fun updateScheduleEndDate(
      taskIdentifier: TaskId,
      schedule: TaskScheduleSnapshot,
      endDate: LocalDate?
  ): LocalDate? {
    return if (schedule.end != endDate) {
      if (taskAuthorizationComponent.hasEditPermissionOnTask(taskIdentifier)) {
        endDate
      } else {
        throw AccessDeniedException("User not permitted to edit task dates")
      }
    } else {
      schedule.end
    }
  }

  private fun updateSlots(
      taskIdentifier: TaskId,
      schedule: TaskScheduleSnapshot,
      newSlotDates: Map<DayCardId, LocalDate>?
  ): List<TaskScheduleSlotDto> {
    taskScheduleCommandHandlerHelper.validateDayCards(newSlotDates, schedule)

    val changedSlots: MutableMap<DayCardId, LocalDate> =
        taskScheduleCommandHandlerHelper.getChangedSlots(schedule, newSlotDates)

    return if (changedSlots.isNotEmpty()) {
      if (taskAuthorizationComponent.hasContributePermissionOnTask(taskIdentifier)) {
        val newSlotList = mutableListOf<TaskScheduleSlotDto>()
        newSlotList.addAll(schedule.slots!!)
        for (slot in newSlotList) {
          slot.date = changedSlots.getOrDefault(slot.id, slot.date)
        }
        taskScheduleCommandHandlerHelper.updateSlotOrder(newSlotList)
        newSlotList.map { TaskScheduleSlotDto(it.id, it.date) }
      } else {
        throw AccessDeniedException("User not permitted to edit day card dates")
      }
    } else {
      schedule.slots?.map { TaskScheduleSlotDto(it.id, it.date) } ?: listOf()
    }
  }
}
