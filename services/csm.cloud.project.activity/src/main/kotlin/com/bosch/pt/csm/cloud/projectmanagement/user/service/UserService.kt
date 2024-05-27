/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.service

import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

  @Trace fun save(user: User): User = userRepository.save(user)

  @Trace fun findOneCached(identifier: UUID) = userRepository.findOneCachedByIdentifier(identifier)

  @Trace
  fun findOneCached(externalIdentifier: String) =
      userRepository.findOneCachedByExternalIdentifier(externalIdentifier)

  @Trace fun delete(identifier: UUID) = userRepository.deleteUser(identifier)

  @Trace
  fun savePicture(identifier: UUID, pictureIdentifier: UUID) =
      userRepository.savePicture(identifier, pictureIdentifier)

  @Trace fun deletePicture(userIdentifier: UUID) = userRepository.deletePicture(userIdentifier)
}
