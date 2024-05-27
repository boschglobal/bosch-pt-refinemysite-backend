/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper.Companion.getInstance
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class SecurityContextHelperTest {

  private lateinit var currentUser: User

  @BeforeEach
  fun init() {
    currentUser = user().withUserId("myuser").asAdmin().build()
    SecurityContextHolder.getContext().authentication = createAuthentication(currentUser)
  }

  /** Verify user has given role. */
  @Test
  fun hasRole() {
    val hasRole = getInstance().hasRole("ADMIN")
    assertThat(hasRole).isTrue
  }

  /** Verify user has no role without authentication. */
  @Test
  fun hasRoleNoAuthentication() {
    SecurityContextHolder.getContext().authentication = null
    val hasRole = getInstance().hasRole("ADMIN")
    assertThat(hasRole).isFalse
  }

  /** Verify role matching with role prefix given. */
  @Test
  fun hasRoleWithPrefix() {
    val hasRole = getInstance().hasRole("ROLE_ADMIN")
    assertThat(hasRole).isTrue
  }

  /** Verify user does not have given role. */
  @Test
  fun hasNoRole() {
    val hasRole = getInstance().hasRole("DUMMY")
    assertThat(hasRole).isFalse
  }

  /** Verify error handling when no user is given. */
  @Test
  fun hasRoleWithoutRoleGiven() {
    val hasRole = getInstance().hasRole(null)
    assertThat(hasRole).isFalse
  }

  /** Verify that user has any of the given roles. */
  @Test
  fun hasAnyRole() {
    val hasAnyRole = getInstance().hasAnyRole("USER", "ADMIN")
    assertThat(hasAnyRole).isTrue
  }

  /** Verify expected current user is returned. */
  @Test
  fun getCurrentUser() {
    val user = getInstance().getCurrentUser()
    assertThat(user).isNotNull.isEqualTo(currentUser)
  }

  private fun createAuthentication(principal: User?): Authentication =
      if (principal == null) {
        AnonymousAuthenticationToken(
            "anonymous", "anonymous", listOf(SimpleGrantedAuthority("USER")))
      } else {
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
      }
}
