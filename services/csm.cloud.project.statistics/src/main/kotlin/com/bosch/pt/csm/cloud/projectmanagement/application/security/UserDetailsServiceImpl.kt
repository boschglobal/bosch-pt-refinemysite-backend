/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.UserService
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@Qualifier("StatisticsUserDetails")
class UserDetailsServiceImpl @Autowired constructor(private val userService: UserService) :
    UserDetailsService {

  @Throws(UsernameNotFoundException::class)
  override fun loadUserByUsername(userId: String): UserDetails =
      when (StringUtils.isBlank(userId)) {
        true -> throw UsernameNotFoundException("Empty userId given")
        else -> {
          val user =
              userService.findOneByUserId(userId)
                  ?: throw UsernameNotFoundException("No user found for userId $userId")

          if (!user.isAccountNonLocked)
              throw LockedException("The user account with userId $userId is locked")

          user
        }
      }
}
