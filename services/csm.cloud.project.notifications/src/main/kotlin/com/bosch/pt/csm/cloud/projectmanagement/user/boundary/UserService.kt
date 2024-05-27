/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.boundary

import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import datadog.trace.api.Trace
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

  @Trace fun save(user: User): User = userRepository.save(user)

  @Trace
  fun saveUserPictureIdentifier(userIdentifier: UUID, userPictureIdentifier: UUID) =
      userRepository.saveUserPictureIdentifier(userIdentifier, userPictureIdentifier)

  @Trace
  fun setLastSeen(userIdentifier: UUID, date: Date) =
      userRepository.saveLastSeen(userIdentifier, date)

  @Trace
  fun getLastSeen(userIdentifier: UUID): Instant? = userRepository.findLastSeen(userIdentifier)

  @Trace
  fun findOneCachedByIdentifier(userIdentifier: UUID): User? =
      userRepository.findOneCachedByIdentifier(userIdentifier)

  @Trace
  fun findOneCachedByExternalIdentifier(userExternalIdentifier: String): User? =
      userRepository.findOneCachedByExternalIdentifier(userExternalIdentifier)

  @Trace fun deleteUser(userIdentifier: UUID) = userRepository.deleteUser(userIdentifier)

  @Trace
  fun deleteUserPictureIdentifier(userIdentifier: UUID) =
      userRepository.deleteUserPictureIdentifier(userIdentifier)
}
