/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("TaskUserDetails")
class UserDetailsServiceImpl(private val userQueryService: UserQueryService) : UserDetailsService {

  override fun loadUserByUsername(userId: String): UserDetails {
    if (userId.isBlank()) {
      throw UsernameNotFoundException("Empty userId given")
    }

    val userDetails: UserDetails =
        (userQueryService.findOneWithPictureByUserId(userId)
            ?: throw UsernameNotFoundException("No user found for userId $userId"))

    if (!userDetails.isAccountNonLocked) {
      throw LockedException("The user account with userId $userId is locked")
    }

    return userDetails
  }
}
