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
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.DOCUMENT_VALIDATION_ERROR_INVALID_INCREMENT
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentVersionIncrementedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.IncrementDocumentVersionCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IncrementDocumentVersionCommandHandler(
    val snapshotStore: DocumentSnapshotStore,
    val eventBus: ConsentsContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: IncrementDocumentVersionCommand) {
    val now = LocalDateTime.now()
    snapshotStore
        .findOrFail(command.documentIdentifier)
        .toCommandHandler()
        .checkPrecondition { document ->
          document.versions.all { it.lastChanged.isBefore(command.version.lastChanged) }
        }
        .onFailureThrow(DOCUMENT_VALIDATION_ERROR_INVALID_INCREMENT)
        .emitEvent(
            DocumentVersionIncrementedEvent(
                command.documentIdentifier,
                command.version,
                now,
                AuditUserExtractor.getCurrentUserReference().identifier))
        .to(eventBus)
  }
}
