/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskAssignmentCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AssignTaskBatchCommandHandler(
    private val assignTaskCommandHandler: AssignTaskCommandHandler,
    private val participantRepository: ParticipantRepository,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(
      projectIdentifier: ProjectId,
      commands: List<UpdateTaskAssignmentCommand>,
  ) {
    if (commands.isEmpty()) {
      return
    }

    val assignees = commands.map { it.assigneeIdentifier }
    // Populate existence cache
    if (assignees.isNotEmpty()) {
      participantRepository.findAllByIdentifierIn(assignees)
    }

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      commands.map { assignTaskCommandHandler.handle(it.identifier, it.assigneeIdentifier) }
    }
  }
}
