/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.image.messages.ImageDeletedEventAvro
import com.bosch.pt.csm.cloud.image.messages.ImageScaledEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.DeleteProfilePictureOfUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.UpdateImageMetadataCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.asProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler.DeleteProfilePictureCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler.UpdateImageMetadataCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.picture.query.ProfilePictureQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import java.util.regex.Pattern
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ImageScalingProcessor(
    private val deleteProfilePictureCommandHandler: DeleteProfilePictureCommandHandler,
    private val profilePictureQueryService: ProfilePictureQueryService,
    private val updateImageMetadataCommandHandler: UpdateImageMetadataCommandHandler,
    private val userQueryService: UserQueryService,
) {
  fun process(message: ImageScaledEventAvro) {
    val profilePictureMatcher = PROFILE_PICTURE_PATTERN.matcher(message.path)
    if (profilePictureMatcher.find()) {
      val userId = profilePictureMatcher.group(1).asUserId()
      val userExists = userExists(userId)

      val profilePictureId = message.filename.toUUID().asProfilePictureId()
      val pictureExists = pictureExists(profilePictureId)
      // Picture could be deleted meanwhile (if async processing is delayed).
      // Skip update as it throws an exception if the picture doesn't exit.
      // Log only a warning if the picture does not exist.
      if (userExists && pictureExists) {
        updateImageMetadataCommandHandler.handle(
            UpdateImageMetadataCommand(profilePictureId, message.contentLength))
      } else {
        LOGGER.warn(
            "Profile picture with id $profilePictureId not found to update available resolutions")
      }
    }
  }

  fun delete(message: ImageDeletedEventAvro) {
    val profilePictureMatcher = PROFILE_PICTURE_PATTERN.matcher(message.path)
    if (profilePictureMatcher.find()) {
      val userId = profilePictureMatcher.group(1).asUserId()
      val userExists = userExists(userId)

      val profilePictureId = message.filename.toUUID().asProfilePictureId()
      val pictureExists = pictureExists(profilePictureId)

      if (userExists && pictureExists) {
        deleteProfilePictureCommandHandler.handleWithoutAuthorization(
            DeleteProfilePictureOfUserCommand(userId, false))
      } else {
        LOGGER.warn("Profile picture with id $profilePictureId not found to delete")
      }
    }
  }

  private fun pictureExists(profilePictureId: ProfilePictureId) =
      profilePictureQueryService.findOneWithDetailsByIdentifier(profilePictureId) != null

  private fun userExists(userId: UserId) = userQueryService.findOneByUserId(userId) != null

  companion object {
    private val PROFILE_PICTURE_PATTERN = Pattern.compile("/images/users/([^/]*)/picture")
    private val LOGGER = LoggerFactory.getLogger(ImageScalingProcessor::class.java)
  }
}
