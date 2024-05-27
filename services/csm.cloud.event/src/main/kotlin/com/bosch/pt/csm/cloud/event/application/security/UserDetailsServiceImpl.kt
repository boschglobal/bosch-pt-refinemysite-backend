/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.security

import com.bosch.pt.csm.cloud.event.user.boundary.UserBoundaryService
import com.bosch.pt.csm.cloud.event.user.model.User
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@Qualifier("eventUserDetails")
class UserDetailsServiceImpl(private val userBoundaryService: UserBoundaryService) :
    ReactiveUserDetailsService {

  override fun findByUsername(username: String): Mono<UserDetails> {
    if (username.isBlank()) {
      throw UsernameNotFoundException("Empty userId given")
    }
    return userBoundaryService.findCurrentUser().map { user: User -> user }
  }
}
