/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.boundary.UserService
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("TaskUserDetails")
class UserDetailsServiceImpl(private val userService: UserService) : UserDetailsService {

  @Throws(UsernameNotFoundException::class)
  override fun loadUserByUsername(userId: String): UserDetails {
    if (StringUtils.isBlank(userId)) {
      throw UsernameNotFoundException("Empty userId given")
    }

    val user = userService.findOneWithPictureByUserId(userId)
    if (user == null || user.deleted) {
      throw UsernameNotFoundException("No user found for userId $userId")
    }

    if (!user.isAccountNonLocked) {
      throw LockedException("The user account with userId $userId is locked")
    }

    return user
  }
}
