/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.attachment.boundary.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.DeleteProfilePictureOfUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshotStore
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteProfilePictureCommandHandler(
    private val blobStoreService: BlobStoreService,
    private val snapshotStore: ProfilePictureSnapshotStore,
    private val eventBus: UserContextLocalEventBus
) {

  @PreAuthorize(
      "hasRole('ADMIN') || @userAuthorizationComponent.isCurrentUser(#command.userIdentifier) || " +
        "@userAuthorizationComponent.isCurrentUserSystemUser()")
  @Transactional
  fun handle(command: DeleteProfilePictureOfUserCommand) {
    try {
      snapshotStore
          .findSnapshotByUserIdentifierOrFail(command.userIdentifier)
          .toCommandHandler()
          .emitTombstone()
          .to(eventBus)
          .apply { withSideEffects { deleteImageFiles(it) } }
    } catch (exception: AggregateNotFoundException) {
      if (command.failIfNotExists) throw exception
    }
  }

  @DenyWebRequests
  @NoPreAuthorize
  @Transactional
  fun handleWithoutAuthorization(command: DeleteProfilePictureOfUserCommand) {
    try {
      snapshotStore
          .findSnapshotByUserIdentifierOrFail(command.userIdentifier)
          .toCommandHandler()
          .emitTombstone()
          .to(eventBus)
          .apply { withSideEffects { deleteImageFiles(it) } }
    } catch (exception: AggregateNotFoundException) {
      if (command.failIfNotExists) throw exception
    }
  }

  private fun deleteImageFiles(snapshot: ProfilePictureSnapshot) {
    // this could be done async based on events in the future which will also makes this more
    // failsafe in case an error occurs here
    ImageResolution.values().forEach { resolution ->
      blobStoreService.deleteImageIfExists(snapshot, resolution)
    }
  }
}
