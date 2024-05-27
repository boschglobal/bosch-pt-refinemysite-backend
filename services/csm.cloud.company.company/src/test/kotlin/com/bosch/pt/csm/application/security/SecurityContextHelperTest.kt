/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.user.user.model.UserBuilder
import com.bosch.pt.csm.user.user.query.UserProjection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class SecurityContextHelperTest {

  private lateinit var currentUser: UserProjection

  @BeforeEach
  fun init() {
    currentUser = UserBuilder.user().withUserId("myuser").asAdmin().build()
    SecurityContextHolder.getContext().authentication = createAuthentication(currentUser)
  }

  @Test
  fun `verify user has given role`() {
    val hasRole = SecurityContextHelper.hasRole("ADMIN")
    assertThat(hasRole).isTrue
  }

  @Test
  fun `verify user has no role without authentication`() {
    SecurityContextHolder.getContext().authentication = null
    val hasRole = SecurityContextHelper.hasRole("ADMIN")
    assertThat(hasRole).isFalse
  }

  @Test
  fun `verify role matching with role prefix given`() {
    val hasRole = SecurityContextHelper.hasRole("ROLE_ADMIN")
    assertThat(hasRole).isTrue
  }

  @Test
  fun `verify user does not have given role`() {
    val hasRole = SecurityContextHelper.hasRole("DUMMY")
    assertThat(hasRole).isFalse
  }

  @Test
  fun `verify that user has any of the given roles`() {
    val hasAnyRole = SecurityContextHelper.hasAnyRole("USER", "ADMIN")
    assertThat(hasAnyRole).isTrue
  }

  @Test
  fun `verify expected current user is returned`() {
    val user = SecurityContextHelper.getCurrentUser()
    assertThat(user).isNotNull.isEqualTo(currentUser)
  }

  private fun createAuthentication(principal: UserProjection?): Authentication =
      if (principal == null) {
        AnonymousAuthenticationToken(
            "anonymous", "anonymous", listOf(SimpleGrantedAuthority("USER")))
      } else {
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
      }
}
