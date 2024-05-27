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
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.SendTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import org.springframework.stereotype.Component

@Component
open class TaskCreateService(
    private val assignTaskCommandHandler: AssignTaskCommandHandler,
    private val sendTaskCommandHandler: SendTaskCommandHandler
) {

  fun changeAssignAndStatusIfRequired(
      identifier: TaskId,
      assignee: ParticipantId?,
      status: TaskStatusEnum
  ) {

    if (assignee != null) {
      assignTaskCommandHandler.handle(identifier = identifier, assigneeIdentifier = assignee)

      if (status === TaskStatusEnum.OPEN) {
        sendTaskCommandHandler.handle(identifier)
      }
    }
  }
}
