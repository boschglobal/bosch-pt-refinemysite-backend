/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandlerChangeDefinition
import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_USER_NOT_LOCKING_THEMSELVES
import com.bosch.pt.csm.cloud.usermanagement.user.authorization.boundary.AdminUserAuthorizationService
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.LockUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UnlockUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshotStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LockUnlockUserCommandHandler(
    private val eventBus: UserContextLocalEventBus,
    private val snapshotStore: UserSnapshotStore,
    private val adminUserAuthorizationService: AdminUserAuthorizationService,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: LockUserCommand): UserId =
      snapshotStore.findOrFail(command.identifier).toCommandHandler().emitLocked(true)

  @AdminAuthorization
  @Transactional
  fun handle(command: UnlockUserCommand): UserId =
      snapshotStore.findOrFail(command.identifier).toCommandHandler().emitLocked(false)

  private fun CommandHandlerChangeDefinition<UserSnapshot>.emitLocked(locked: Boolean): UserId =
      this.checkAuthorization { adminUserAuthorizationService.authorizedForCountry(it.country) }
          .onFailureThrow {
            AccessDeniedException("Admin user not authorized for country of this user.")
          }
          .checkPrecondition { isNotSystemUser(it) }
          .onFailureThrow(USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED)
          .checkPrecondition { isNotCurrentUser(it) }
          .onFailureThrow(USER_VALIDATION_ERROR_USER_NOT_LOCKING_THEMSELVES)
          .applyChanges { it.locked = locked }
          .emitEvent(UPDATED)
          .ifSnapshotWasChanged()
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun isNotSystemUser(it: UserSnapshot) = it.identifier != systemUserIdentifier

  private fun isNotCurrentUser(it: UserSnapshot) =
      it.identifier != SecurityContextHelper.getCurrentUser().identifier
}
