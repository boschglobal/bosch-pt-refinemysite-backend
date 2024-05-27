/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("NewsUserDetails")
class UserDetailsServiceImpl(private val userService: UserService) : UserDetailsService {

  @Throws(UsernameNotFoundException::class)
  override fun loadUserByUsername(userId: String): UserDetails {
    if (userId.isBlank()) {
      throw UsernameNotFoundException("Empty userId given")
    }

    val user =
        userService.findOneByUserId(userId)
            ?: throw UsernameNotFoundException(
                "No user found for userId $userId",
            )

    if (!user.isAccountNonLocked) {
      throw LockedException("The user account with userId $userId is locked")
    }

    return user
  }
}
