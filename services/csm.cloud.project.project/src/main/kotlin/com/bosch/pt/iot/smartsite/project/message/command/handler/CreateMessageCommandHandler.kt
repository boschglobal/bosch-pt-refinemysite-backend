/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.message.command.api.CreateMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.MessageSnapshot
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateMessageCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
) {

  @Trace
  @Transactional
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#command.topicIdentifier)")
  fun handle(command: CreateMessageCommand): MessageId {
    return MessageSnapshot(
            identifier = command.identifier,
            version = INITIAL_SNAPSHOT_VERSION,
            content = command.content,
            topicIdentifier = command.topicIdentifier,
            projectIdentifier = command.projectIdentifier)
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
