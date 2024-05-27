/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler

import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateEmailCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshotStore
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateEmailCommandHandler(
    private val eventBus: UserContextLocalEventBus,
    private val snapshotStore: UserSnapshotStore
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUser(#command.identifier)")
  @Transactional
  fun handle(command: UpdateEmailCommand) =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .assertVersionMatches(command.version)
          .applyChanges { user -> user.email = command.email }
          .emitEvent(UserEventEnumAvro.UPDATED)
          .ifSnapshotWasChanged()
          .to(eventBus)
          .let {}
}
