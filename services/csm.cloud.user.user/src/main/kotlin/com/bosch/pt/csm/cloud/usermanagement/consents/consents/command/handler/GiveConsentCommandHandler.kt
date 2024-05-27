/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.CONSENTS_VALIDATION_ERROR_DOCUMENT_DOES_NOT_APPLY_TO_USER
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.GiveConsentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.UserConsentedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore.ConsentsUserSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore.ConsentsUserSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.query.DocumentsQueryService
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshotStore
import java.time.LocalDateTime
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GiveConsentCommandHandler(
    val snapshotStore: ConsentsUserSnapshotStore,
    val eventBus: ConsentsContextLocalEventBus,
    val documentsQueryService: DocumentsQueryService,
    val userSnapshotStore: UserSnapshotStore
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUser(#command.userIdentifier)")
  @Transactional
  fun handle(command: GiveConsentCommand) {
    val now = LocalDateTime.now()
    snapshotStore
        .findOrCreateSnapshot(command.userIdentifier)
        .toCommandHandler()
        .checkPrecondition {
          allDocumentsApplyToUser(command.userIdentifier, command.documentVersionIds)
        }
        .onFailureThrow(CONSENTS_VALIDATION_ERROR_DOCUMENT_DOES_NOT_APPLY_TO_USER)
        .emitEvents { user -> user.consentTo(command.documentVersionIds, now) }
        .to(eventBus)
  }

  private fun allDocumentsApplyToUser(
      userId: UserId,
      documentVersionIds: Collection<DocumentVersionId>
  ): Boolean {
    val currentUser = userSnapshotStore.findOrFail(userId)
    val documents = documentsQueryService.findDocuments(currentUser.country, currentUser.locale)
    return documents
        .flatMap { document -> document.versions.map { it.identifier } }
        .containsAll(documentVersionIds)
  }

  private fun ConsentsUserSnapshot.consentTo(
      documentVersionIds: Collection<DocumentVersionId>,
      timeOfConsent: LocalDateTime
  ) =
      documentVersionIds
          .filter { !hasGivenConsent(it) }
          .map { UserConsentedEvent(identifier, it, timeOfConsent) }
}
