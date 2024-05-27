/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_ALREADY_EXISTS
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshot
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateTaskScheduleCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasEditPermissionOnTask(#command.taskIdentifier)")
  open fun handle(command: CreateTaskScheduleCommand): TaskScheduleId {
    val task = taskScheduleCommandHandlerHelper.findTaskOrFail(command)

    if (task.taskSchedule != null) {
      throw PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_ALREADY_EXISTS)
    }

    val slots = command.slots?.map { TaskScheduleSlotDto(it.key, it.value) } ?: listOf()
    taskScheduleCommandHandlerHelper.validateTaskSchedule(command.start, command.end, slots)

    taskScheduleCommandHandlerHelper.updateSlotOrder(slots.toMutableList())
    val project = taskScheduleCommandHandlerHelper.findProjectOrFail(task.project.identifier)

    return TaskScheduleSnapshot(
            identifier = command.identifier,
            version = INITIAL_SNAPSHOT_VERSION,
            projectIdentifier = project.identifier,
            start = command.start,
            end = command.end,
            taskIdentifier = task.identifier,
            slots = slots)
        .toCommandHandler()
        .emitEvent(TaskScheduleEventEnumAvro.CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
