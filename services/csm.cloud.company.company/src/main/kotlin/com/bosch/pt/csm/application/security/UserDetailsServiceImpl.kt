/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.user.user.query.UserQueryService
import org.apache.commons.lang3.StringUtils.isBlank
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("CompanyUserDetails")
class UserDetailsServiceImpl : UserDetailsService {

  @Autowired private lateinit var userQueryService: UserQueryService

  override fun loadUserByUsername(userId: String): UserDetails {
    if (isBlank(userId)) {
      throw UsernameNotFoundException("Empty userId given")
    }

    val user =
        userQueryService.findOneByCiamUserId(userId)
            ?: throw UsernameNotFoundException("No user found for userId $userId")

    if (!user.isAccountNonLocked) {
      throw LockedException("The user account with userId $userId is locked")
    }

    return user
  }
}
