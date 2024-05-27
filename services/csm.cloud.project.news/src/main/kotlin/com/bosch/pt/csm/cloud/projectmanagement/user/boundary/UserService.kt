/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.user.boundary

import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserService(private val userRepository: UserRepository) {

  @Trace
  fun createOrUpdate(userId: String, identifier: UUID, admin: Boolean, locked: Boolean) {
    val user = userRepository.findOneByIdentifier(identifier)

    if (user == null) {
      userRepository.save(User(userId, identifier, admin, locked))
    } else {
      user.userId = userId
      user.admin = admin
      user.locked = locked

      userRepository.save(user)
    }
  }

  @Trace
  @Transactional(readOnly = true)
  fun findOneByUserId(userId: String): User? = userRepository.findOneByUserId(userId)

  @Trace fun delete(identifier: UUID): Long = userRepository.deleteByIdentifier(identifier)
}
