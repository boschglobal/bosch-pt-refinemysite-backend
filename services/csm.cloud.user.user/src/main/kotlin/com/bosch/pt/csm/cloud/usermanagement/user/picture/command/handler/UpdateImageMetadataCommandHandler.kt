/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.attachment.boundary.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.attachment.util.ImageMetadataExtractor
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.UpdateImageMetadataCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshotStore
import java.util.TimeZone
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateImageMetadataCommandHandler(
    private val logger: Logger,
    private val blobStoreService: BlobStoreService,
    private val eventBus: UserContextLocalEventBus,
    private val imageMetadataExtractor: ImageMetadataExtractor,
    private val snapshotStore: ProfilePictureSnapshotStore,
) {

  @DenyWebRequests
  @NoPreAuthorize
  @Transactional
  fun handle(command: UpdateImageMetadataCommand) {

    val snapshot = snapshotStore.findOrFail(command.identifier)

    val imageMetadata =
        try {
          // Load the original picture to get the full image resolution
          blobStoreService
              .read(
                  "user/image/original/${snapshot.user.identifier}/${snapshot.identifier.identifier}")
              .use { it?.readBytes() }
              ?.let { imageMetadataExtractor.readMetadata(it, TimeZone.getDefault()) }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
          logger.trace("Couldn't read image metadata", e)
          null
        }

    snapshot
        .toCommandHandler()
        .applyChanges { picture: ProfilePictureSnapshot ->
          picture.fileSize = command.fileSize
          picture.height = imageMetadata?.imageHeight
          picture.width = imageMetadata?.imageWidth
          picture.setResolutionAvailable(SMALL)
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
  }
}
