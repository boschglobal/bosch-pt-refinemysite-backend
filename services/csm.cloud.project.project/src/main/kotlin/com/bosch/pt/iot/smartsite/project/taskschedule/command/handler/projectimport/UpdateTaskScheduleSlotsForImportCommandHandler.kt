/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.projectimport

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleSlotsForImportCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshotStore
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateTaskScheduleSlotsForImportCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: TaskScheduleSnapshotStore,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Transactional
  @NoPreAuthorize
  open fun handle(command: UpdateTaskScheduleSlotsForImportCommand) {
    val taskSchedule =
        requireNotNull(taskScheduleRepository.findOneByTaskIdentifier(command.taskIdentifier))

    val slots =
        command.slots.map { TaskScheduleSlotDto(date = it.value, id = it.key) }.toMutableList()

    taskScheduleCommandHandlerHelper.validateTaskSchedule(
        taskSchedule.start, taskSchedule.end, slots)

    taskScheduleCommandHandlerHelper.updateSlotOrder(slots)

    snapshotStore
        .findOrFail(taskSchedule.identifier)
        .toCommandHandler()
        .update { it.copy(slots = slots) }
        .emitEvent(TaskScheduleEventEnumAvro.UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
