/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler

import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor.getCurrentUserReference
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.PAT_VALIDATION_ERROR_USER_MISMATCH
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.PAT_VALIDATION_ERROR_VALIDITY_OUT_OF_BOUNDS
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.MAX_PAT_VALIDITY_IN_MINUTES
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.MIN_PAT_VALIDITY_IN_MINUTES
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatUpdatedEvent
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.UpdatePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.snapshotstore.PatSnapshotStore
import java.time.LocalDateTime.now
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdatePatCommandHandler(
    private val snapshotStore: PatSnapshotStore,
    private val eventBus: PatLocalEventBus
) {

  @Suppress("SpringElInspection")
  @PreAuthorize("#command.impersonatedUser == principal.identifier")
  @Transactional
  fun handle(command: UpdatePatCommand): PatId =
      CommandHandler.of(snapshotStore.findOrFail(command.patId))
          .assertVersionMatches(command.version)
          .checkAuthorization { command.impersonatedUser == it.impersonatedUser }
          .onFailureThrow(PAT_VALIDATION_ERROR_USER_MISMATCH)
          .checkPrecondition {
            command.validForMinutes in MIN_PAT_VALIDITY_IN_MINUTES..MAX_PAT_VALIDITY_IN_MINUTES
          }
          .onFailureThrow(PAT_VALIDATION_ERROR_VALIDITY_OUT_OF_BOUNDS)
          .applyChanges { snapshot ->
            snapshot.description = command.description
            snapshot.expiresAt = snapshot.issuedAt.plusMinutes(command.validForMinutes)
            snapshot.scopes = command.scopes
          }
          .emitEvent { originalSnapshot ->
            PatUpdatedEvent(
                patId = command.patId,
                impersonatedUser = command.impersonatedUser,
                scopes = command.scopes,
                description = command.description,
                expiresAt = originalSnapshot.issuedAt.plusMinutes(command.validForMinutes),
                userIdentifier = getCurrentUserReference().identifier,
                timestamp = now(),
            )
          }
          .to(eventBus)
          .andReturnSnapshot()
          .identifier
}
