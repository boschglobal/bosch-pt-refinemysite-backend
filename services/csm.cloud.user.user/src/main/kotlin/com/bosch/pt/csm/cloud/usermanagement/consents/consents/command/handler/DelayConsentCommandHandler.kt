/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.handler

import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.ConsentDelayedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.DelayConsentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore.ConsentsUserSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import java.time.LocalDateTime
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DelayConsentCommandHandler(
    val snapshotStore: ConsentsUserSnapshotStore,
    val eventBus: ConsentsContextLocalEventBus
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUser(#command.userIdentifier)")
  @Transactional
  fun handle(command: DelayConsentCommand) {
    val now = LocalDateTime.now()
    snapshotStore
        .findOrCreateSnapshot(command.userIdentifier)
        .toCommandHandler()
        .emitEvent(ConsentDelayedEvent(command.userIdentifier, now))
        .to(eventBus)
  }
}
