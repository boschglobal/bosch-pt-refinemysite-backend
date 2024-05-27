/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.message.command.api.DeleteMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.MessageSnapshotStore
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteMessageCommandHandler(
    private val snapshotStore: MessageSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val logger: Logger,
    private val messageRepository: MessageRepository,
) {

  @Trace
  @Transactional
  @PreAuthorize("@messageAuthorizationComponent.hasDeletePermissionOnMessage(#command.identifier)")
  fun handle(command: DeleteMessageCommand) {
    messageRepository.findOneWithDetailsByIdentifier(command.identifier)
        ?: throw AccessDeniedException("User has no access to this message")

    snapshotStore
        .findOrIgnore(command.identifier)
        ?.toCommandHandler()
        ?.emitEvent(DELETED)
        ?.to(eventBus) ?: logMessageNotFound(command.identifier)
  }

  @Trace
  @Transactional
  @NoPreAuthorize
  fun handleWithoutAuthorization(command: DeleteMessageCommand) {
    snapshotStore
        .findOrIgnore(command.identifier)
        ?.toCommandHandler()
        ?.emitEvent(DELETED)
        ?.to(eventBus) ?: logMessageNotFound(command.identifier)
  }

  private fun logMessageNotFound(identifier: MessageId) =
      logger.warn(
          "A message was consumed to delete message {} but it was not found. " +
              "Most likely it was already deleted but the offset could not be committed.",
          identifier)
}
