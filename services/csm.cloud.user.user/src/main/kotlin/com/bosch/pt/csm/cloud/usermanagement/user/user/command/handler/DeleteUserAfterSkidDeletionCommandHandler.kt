/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.DeleteProfilePictureOfUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler.DeleteProfilePictureCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.DeleteUserAfterSkidDeletionCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshotStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteUserAfterSkidDeletionCommandHandler(
    private val eventBus: UserContextLocalEventBus,
    private val profilePictureCommandHandler: DeleteProfilePictureCommandHandler,
    private val snapshotStore: UserSnapshotStore,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUserSystemUser()")
  @Transactional
  fun handle(command: DeleteUserAfterSkidDeletionCommand) =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .checkPrecondition { isNotSystemUser(it) }
          .onFailureThrow(USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED)
          .emitTombstone()
          .also {
            // TODO: In the future, this will happen AFTER the user has been deleted as a
            // consequence, not a pre-requisite. This can happen in an event listener then. On the
            // other hand, the profile picture snapshot is deleted in the user snapshot store
            // anyway. Do we need this call here to raise the event? Or can we skip is?
            profilePictureCommandHandler.handle(
                DeleteProfilePictureOfUserCommand(command.identifier, false))
          }
          .to(eventBus)

  private fun isNotSystemUser(it: UserSnapshot) = it.identifier != systemUserIdentifier
}
