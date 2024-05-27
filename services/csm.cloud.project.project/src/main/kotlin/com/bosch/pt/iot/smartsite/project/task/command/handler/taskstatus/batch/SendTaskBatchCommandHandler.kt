/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.SendTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class SendTaskBatchCommandHandler(
    private val sendTaskCommandHandler: SendTaskCommandHandler,
    private val taskRepository: TaskRepository,
    private val snapshotStore: TaskSnapshotStore,
    private val participantRepository: ParticipantRepository,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(projectIdentifier: ProjectId, identifiers: List<TaskId>) {
    val tasks = taskRepository.findAllByIdentifierIn(identifiers)
    if (tasks.isEmpty()) return

    val assignees = tasks.map { it.assignee?.identifier }

    // Populate existence cache
    participantRepository.findAllByIdentifierIn(assignees)

    // Populate snapshot cache
    snapshotStore.findAllOrIgnore(identifiers)

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      identifiers.map { sendTaskCommandHandler.handle(it) }
    }
  }
}
