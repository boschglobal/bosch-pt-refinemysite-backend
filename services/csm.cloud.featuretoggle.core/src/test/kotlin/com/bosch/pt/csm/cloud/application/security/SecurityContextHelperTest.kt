/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import com.bosch.pt.csm.cloud.user.query.UserProjection
import com.bosch.pt.csm.cloud.user.query.UserProjectionBuilder
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
    currentUser = UserProjectionBuilder.user().withCiamUserId("myuser").asAdmin().build()
    SecurityContextHolder.getContext().authentication = createAuthentication(currentUser)
  }

  @Test
  fun hasRole() {
    val hasRole = SecurityContextHelper.hasRole("ADMIN")
    assertThat(hasRole).isTrue
  }

  @Test
  fun hasRoleNoAuthentication() {
    SecurityContextHolder.getContext().authentication = null
    val hasRole = SecurityContextHelper.hasRole("ADMIN")
    assertThat(hasRole).isFalse
  }

  @Test
  fun hasRoleWithPrefix() {
    val hasRole = SecurityContextHelper.hasRole("ROLE_ADMIN")
    assertThat(hasRole).isTrue
  }

  @Test
  fun hasNoRole() {
    val hasRole = SecurityContextHelper.hasRole("DUMMY")
    assertThat(hasRole).isFalse
  }

  @Test
  fun hasAnyRole() {
    val hasAnyRole = SecurityContextHelper.hasAnyRole("USER", "ADMIN")
    assertThat(hasAnyRole).isTrue
  }

  @Test
  fun getCurrentUser() {
    val user = SecurityContextHelper.getCurrentUser()
    assertThat(user).isNotNull.isEqualTo(currentUser)
  }

  private fun createAuthentication(principal: UserProjection?): Authentication =
      if (principal == null)
          AnonymousAuthenticationToken(
              "anonymous", "anonymous", listOf(SimpleGrantedAuthority("USER")))
      else UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
}
