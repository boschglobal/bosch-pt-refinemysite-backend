/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
import com.bosch.pt.iot.smartsite.user.repository.ProfilePictureRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class ProfilePictureService(private val profilePictureRepository: ProfilePictureRepository) {

  @Trace
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  open fun save(profilePicture: ProfilePicture): UUID =
      with(profilePicture) {
        require(profilePicture.user != null) { "User must not be null" }
        val existingPicture =
            profilePictureRepository.findOneByUserIdentifier(profilePicture.user!!.identifier!!)

        if (existingPicture != null) {
          profilePicture.id = existingPicture.id
        }

        profilePictureRepository.save(profilePicture).identifier!!
      }

  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  open fun deleteProfilePictureByUser(userIdentifier: UUID) =
      delete(profilePictureRepository.findOneByUserIdentifier(userIdentifier))

  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  open fun deleteProfilePictureByIdentifier(identifier: UUID) =
      delete(profilePictureRepository.findOneByIdentifier(identifier))

  private fun delete(profilePicture: ProfilePicture?) =
      profilePicture?.let { profilePictureRepository.delete(it) }
}
