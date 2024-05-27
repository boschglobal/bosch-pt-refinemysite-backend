/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.user.service.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("userDetails")
class UserDetailsServiceImpl(private val userService: UserService) : UserDetailsService {

  override fun loadUserByUsername(userId: String) =
      when {
        userId.isBlank() -> throw UsernameNotFoundException("Empty userId given")
        else -> {
          val user =
              userService.findOneCached(userId)
                  ?: throw UsernameNotFoundException("No user found for userId $userId")

          if (!user.isAccountNonLocked)
              throw LockedException("The user account with userId $userId is locked")

          user
        }
      }
}
