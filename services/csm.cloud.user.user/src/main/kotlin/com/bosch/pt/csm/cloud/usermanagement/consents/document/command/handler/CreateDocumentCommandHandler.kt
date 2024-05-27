/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.DOCUMENT_VALIDATION_ERROR_DOCUMENT_ALREADY_EXISTS
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.DOCUMENT_VALIDATION_ERROR_ONLY_LANGUAGE_SUPPORTED
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.CreateDocumentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentCreatedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import java.time.LocalDateTime
import java.util.Locale
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateDocumentCommandHandler(
    private val snapshotStore: DocumentSnapshotStore,
    private val eventBus: ConsentsContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: CreateDocumentCommand): DocumentId {
    val now = LocalDateTime.now()
    // TODO: redesign command handler
    return DocumentSnapshot(
            command.title,
            command.url,
            command.documentType,
            command.country,
            command.locale,
            command.clientSet,
            listOf(command.initialVersion),
            command.documentIdentifier,
            INITIAL_SNAPSHOT_VERSION)
        .toCommandHandler()
        .checkPrecondition {
          !snapshotStore.existsByClientAndDocumentTypeAndCountryAndLocale(
              it.clientSet, it.documentType, it.country, it.locale)
        }
        .onFailureThrow(DOCUMENT_VALIDATION_ERROR_DOCUMENT_ALREADY_EXISTS)
        .checkPrecondition { it.locale == Locale(it.locale.language) }
        .onFailureThrow(DOCUMENT_VALIDATION_ERROR_ONLY_LANGUAGE_SUPPORTED)
        .emitEvent(
            DocumentCreatedEvent(
                command.documentIdentifier,
                command.documentType,
                command.country,
                command.locale,
                command.clientSet,
                command.title,
                command.url,
                command.initialVersion,
                now,
                AuditUserExtractor.getCurrentUserReference().identifier))
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
