/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("userDetails")
class UserDetailsServiceImpl(private val userService: UserService) : UserDetailsService {

  @Throws(UsernameNotFoundException::class)
  override fun loadUserByUsername(userId: String) =
      if (StringUtils.isBlank(userId)) {
        throw UsernameNotFoundException("Empty userId given")
      } else {
        val user =
            userService.findOneCachedByExternalIdentifier(userId)
                ?: throw UsernameNotFoundException("No user found for userId $userId")

        if (!user.isAccountNonLocked) {
          throw LockedException("The user account with userId $userId is locked")
        }

        user
      }
}
