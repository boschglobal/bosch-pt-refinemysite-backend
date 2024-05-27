/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DELETE_NOT_POSSIBLE_DUE_TO_EXISTING_DAY_CARDS
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.DeleteTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshotStore
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteTaskScheduleCommandHandler(
    private val snapshotStore: TaskScheduleSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasEditPermissionOnTask(#command.taskIdentifier)")
  open fun handle(command: DeleteTaskScheduleCommand) {
    val schedule =
        taskScheduleCommandHandlerHelper.findByTaskIdentifierOrFail(command.taskIdentifier)

    checkIfSlotsNotEmpty(schedule)

    snapshotStore
        .findOrFail(schedule.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.eTag.toVersion())
        .emitEvent(DELETED)
        .to(eventBus)
  }

  private fun checkIfSlotsNotEmpty(schedule: TaskSchedule) {
    if (schedule.slots != null && schedule.slots!!.isNotEmpty()) {
      throw PreconditionViolationException(
          TASK_SCHEDULE_VALIDATION_ERROR_DELETE_NOT_POSSIBLE_DUE_TO_EXISTING_DAY_CARDS)
    }
  }
}
