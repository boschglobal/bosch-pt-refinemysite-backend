/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor
import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.ChangeDocumentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentChangedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangeDocumentCommandHandler(
    val snapshotStore: DocumentSnapshotStore,
    val eventBus: ConsentsContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: ChangeDocumentCommand) {
    val now = LocalDateTime.now()
    snapshotStore
        .findOrFail(command.documentIdentifier)
        .toCommandHandler()
        .update { it.copy(title = command.title ?: it.title, url = command.url ?: it.url) }
        .emitEvent(
            DocumentChangedEvent(
                command.documentIdentifier,
                command.title,
                command.url,
                now,
                AuditUserExtractor.getCurrentUserReference().identifier))
        .ifSnapshotWasChanged()
        .to(eventBus)
  }
}
