/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import com.bosch.pt.csm.cloud.usermanagement.attachment.boundary.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.DeleteProfilePictureOfUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.SaveProfilePictureCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshot
import java.util.TimeZone
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SaveProfilePictureCommandHandler(
    private val blobStoreService: BlobStoreService,
    private val eventBus: UserContextLocalEventBus,
    private val deleteProfilePictureCommandHandler: DeleteProfilePictureCommandHandler
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUser(#command.userIdentifier)")
  @Transactional
  fun handle(command: SaveProfilePictureCommand): ProfilePictureId {

    deleteProfilePictureCommandHandler.handle(
        DeleteProfilePictureOfUserCommand(command.userIdentifier, false))

    return ProfilePictureSnapshot(
            identifier = command.identifier,
            createdDate = null,
            createdBy = null,
            lastModifiedDate = null,
            lastModifiedBy = null,
            user = getCurrentUser().toUserReference(),
            smallAvailable = false,
            fullAvailable = false,
            width = null,
            height = null,
            fileSize = command.binaryData.size.toLong())
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
        .withSideEffects { saveImageInBlobStore(command, it) }
        .andReturnSnapshot()
        .identifier
  }

  private fun saveImageInBlobStore(
      command: SaveProfilePictureCommand,
      snapshot: ProfilePictureSnapshot
  ) {
    blobStoreService.saveImage(
        command.binaryData,
        snapshot,
        BlobMetadata.from(command.fileName, TimeZone.getDefault(), snapshot))
  }
}
