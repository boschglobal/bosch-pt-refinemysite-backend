/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.command.service

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.UnassignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.SendTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import org.springframework.stereotype.Component

@Component
open class TaskUpdateService(
    private val assignTaskCommandHandler: AssignTaskCommandHandler,
    private val unassignTaskCommandHandler: UnassignTaskCommandHandler,
    private val sendTaskCommandHandler: SendTaskCommandHandler
) {

  fun changeAssignAndStatusIfRequired(
      identifier: TaskId,
      oldAssignee: ParticipantId?,
      oldStatus: TaskStatusEnum,
      newAssignee: ParticipantId?,
      newStatus: TaskStatusEnum
  ) {
    if (newAssignee == null && oldAssignee != null) {
      unassignTaskCommandHandler.handle(identifier)
    }

    if (newAssignee != null && newAssignee != oldAssignee) {
      assignTaskCommandHandler.handle(identifier, newAssignee)
    }

    if (oldStatus == DRAFT && newStatus == OPEN && newAssignee != null) {
      sendTaskCommandHandler.handle(identifier)
    }
  }
}
