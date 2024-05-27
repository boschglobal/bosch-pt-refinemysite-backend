/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.attachment.boundary.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository.ProfilePictureRepository
import java.net.URL
import jakarta.annotation.Nonnull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfilePictureQueryService(
    private val blobStoreService: BlobStoreService,
    private val profilePictureRepository: ProfilePictureRepository,
) {

  @NoPreAuthorize // TODO: [SMAR-2408] Limit read permissions on profile pictures
  @Transactional(readOnly = true)
  fun findOneWithDetailsByIdentifier(profilePictureIdentifier: ProfilePictureId): ProfilePicture? =
      profilePictureRepository.findOneWithDetailsByIdentifier(profilePictureIdentifier)

  // TODO: [SMAR-2408] Limit read permissions on profile pictures
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findProfilePictureByUser(userIdentifier: UserId): ProfilePicture? =
      profilePictureRepository.findOneWithDetailsByUserIdentifier(userIdentifier)

  @Nonnull
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findProfilePictureByUserIdentifiers(
      userIdentifiers: Set<UserId>
  ): Map<UserId, ProfilePicture> =
      profilePictureRepository.findAllWithDetailsByUserIdentifierIn(userIdentifiers).associateBy {
        it.user.identifier
      }

  // TODO: [SMAR-2408] Limit read permissions on profile pictures
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun generateBlobAccessUrl(identifier: ProfilePictureId, size: ImageResolution): URL {
    val profilePicture =
        profilePictureRepository.findOneWithDetailsByIdentifier(identifier)
            ?: throw AggregateNotFoundException(
                USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND, identifier.toString())

    return blobStoreService.generateSignedUrlForImage(
        profilePicture, profilePicture.getResolutionOrOriginal(size))
        ?: throw AggregateNotFoundException(
            USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND, profilePicture.identifier.toString())
  }
}
