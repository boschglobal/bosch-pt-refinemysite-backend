/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import com.bosch.pt.csm.cloud.user.query.UserProjector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl : UserDetailsService {

  @Autowired private lateinit var userProjector: UserProjector

  override fun loadUserByUsername(userId: String): UserDetails {
    if (userId.isBlank()) {
      throw UsernameNotFoundException("Empty userId given")
    }

    val user =
        userProjector.loadUserProjectionByCiamId(userId)
            ?: throw UsernameNotFoundException("No user found for userId $userId")

    if (!user.isAccountNonLocked) {
      throw LockedException("The user account with userId $userId is locked")
    }

    return user
  }
}
