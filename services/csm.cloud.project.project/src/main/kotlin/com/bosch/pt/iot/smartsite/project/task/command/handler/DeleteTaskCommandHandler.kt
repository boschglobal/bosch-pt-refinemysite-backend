/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.command.api.DeleteTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshot
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val taskRepository: TaskRepository,
    private val logger: Logger
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  open fun handle(command: DeleteTaskCommand) {
    snapshotStore
        .findOrIgnore(command.identifier)
        ?.apply { ensureMarkedAsDeleted(this) }
        ?.toCommandHandler()
        ?.emitEvent(DELETED)
        ?.to(eventBus)
        ?: logTaskNotFound(command.identifier)
  }

  /**
   * This is a fallback to mark a task as deleted, when the commit to the database in the
   * synchronous part of the operation failed, but the message is successfully sent via kafka.
   */
  private fun ensureMarkedAsDeleted(snapshot: TaskSnapshot) {
    if (!snapshot.deleted) {
      taskRepository.findOneByIdentifier(snapshot.identifier)?.apply {
        taskRepository.markAsDeleted(id!!)
      }
    }
  }

  private fun logTaskNotFound(identifier: TaskId) =
      logger.warn(
          "A message was consumed to delete task {} but it was not found. " +
              "Most likely it was already deleted but the offset could not be committed.",
          identifier)
}
