/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.constants.RoleConstants
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/** Task to verify correct functionality of [SpringSecurityUuidAuditorAware]. */
internal class SpringSecurityUuidAuditorAwareTest {

  private val cut = SpringSecurityUuidAuditorAware()

  /** Verifies expected behavior of [SpringSecurityUuidAuditorAware] if authentication is null. */
  @Test
  fun verifyGetCurrentAuditorAuthenticationNull() {
    // to avoid problems with the order of the test executions
    SecurityContextHolder.getContext().authentication = null

    val userIdentifier = cut.currentAuditor.orElse(null)
    assertThat(userIdentifier).isNull()
  }

  /**
   * Verifies expected behavior of [SpringSecurityUuidAuditorAware] if authentication not
   * authenticated.
   */
  @Test
  fun verifyGetCurrentAuditorAuthenticationNotAuthenticated() {
    val authentication =
        AnonymousAuthenticationToken(
            "key", "test", listOf(SimpleGrantedAuthority(RoleConstants.ADMIN.roleName())))

    authentication.isAuthenticated = false
    SecurityContextHolder.getContext().authentication = authentication

    val userIdentifier = cut.currentAuditor.orElse(null)
    assertThat(userIdentifier).isNull()
  }

  /**
   * Verifies expected behavior of [SpringSecurityUuidAuditorAware] if authentication is anonymous.
   */
  @Test
  fun verifyGetCurrentAuditorAuthenticationAnonymousAuthenticated() {
    val authentication =
        AnonymousAuthenticationToken(
            "key", "test", listOf(SimpleGrantedAuthority(RoleConstants.ADMIN.roleName())))

    SecurityContextHolder.getContext().authentication = authentication

    val userIdentifier = cut.currentAuditor.orElse(null)
    assertThat(userIdentifier).isNull()
  }

  /**
   * Verifies expected behavior of [SpringSecurityUuidAuditorAware] if authentication is valid but
   * principle is null.
   */
  @Test
  fun verifyGetCurrentAuditorAuthenticationValidUserNull() {
    val authentication =
        UsernamePasswordAuthenticationToken(
            null, "secret", setOf(SimpleGrantedAuthority(RoleConstants.ADMIN.roleName())))

    SecurityContextHolder.getContext().authentication = authentication

    val userIdentifier = cut.currentAuditor.orElse(null)
    assertThat(userIdentifier).isNull()
  }

  /** Verifies expected behavior of [SpringSecurityUuidAuditorAware] if authentication is valid. */
  @Test
  fun verifyGetCurrentAuditorAuthenticationValid() {
    val user = User().apply { identifier = randomUUID() }
    val authentication =
        UsernamePasswordAuthenticationToken(
            user, "secret", setOf(SimpleGrantedAuthority(RoleConstants.ADMIN.roleName())))

    SecurityContextHolder.getContext().authentication = authentication

    val userIdentifier = cut.currentAuditor.orElse(null)
    assertThat(userIdentifier).isEqualTo(user.identifier)
  }
}
