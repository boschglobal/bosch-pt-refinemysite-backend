/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.handler

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.command.api.DeleteProjectCommand
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteProjectCommandHandler(
    private val snapshotStore: ProjectSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val projectRepository: ProjectRepository,
    private val logger: Logger
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  open fun handle(command: DeleteProjectCommand) {
    snapshotStore
        .findOrIgnore(command.identifier)
        ?.apply { ensureMarkedAsDeleted(this) }
        ?.toCommandHandler()
        ?.emitEvent(DELETED)
        ?.to(eventBus)
        ?: logProjectNotFound(command.identifier)
  }

  // This is a fallback to mark a project as deleted, when the commit to the database in the
  // synchronous part of the operation failed, but the message is successfully sent via
  // kafka.
  private fun ensureMarkedAsDeleted(snapshot: ProjectSnapshot) {
    if (!snapshot.deleted) {
      projectRepository.findOneByIdentifier(snapshot.identifier)?.apply {
        projectRepository.markAsDeleted(id!!)
      }
    }
  }

  private fun logProjectNotFound(identifier: ProjectId) =
      logger.warn(
          "A message was consumed to delete project $identifier but it was not found. " +
              "Most likely it was already deleted but the offset could not be committed.")
}
