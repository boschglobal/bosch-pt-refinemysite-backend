/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.UserRepository
import java.util.Locale
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

  @Transactional
  fun createOrUpdate(
      userId: String,
      identifier: UUID,
      admin: Boolean,
      locked: Boolean,
      locale: Locale?
  ) {
    val user = userRepository.findOneByIdentifier(identifier)
    if (user == null) {
      userRepository.save(User(userId, identifier, admin, locked, locale))
    } else {
      user.userId = userId
      user.admin = admin
      user.locked = locked
      user.locale = locale
      userRepository.save(user)
    }
  }

  @Transactional(readOnly = true)
  fun findOneByUserId(userId: String): User? {
    return userRepository.findOneByUserId(userId)
  }

  fun delete(identifier: UUID): Long {
    return userRepository.deleteByIdentifier(identifier)
  }
}
