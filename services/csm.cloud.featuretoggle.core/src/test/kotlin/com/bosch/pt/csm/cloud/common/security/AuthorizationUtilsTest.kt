/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.security

import com.bosch.pt.csm.cloud.application.security.AuthorizationUtils
import com.bosch.pt.csm.cloud.application.security.AuthorizationUtils.checkAuthorizationForMultipleResults
import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.user.constants.RoleConstants
import com.bosch.pt.csm.cloud.user.query.UserProjection
import com.bosch.pt.csm.cloud.user.query.UserProjectionBuilder
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.function.BiFunction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder.getContext

@SmartSiteMockKTest
class AuthorizationUtilsTest {

  @AfterEach
  fun cleanup() {
    getContext().authentication = null
  }

  private fun setAuthenticationContext(): UserProjection {
    val principal = UserProjectionBuilder.user().build()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    getContext().authentication = authentication
    return principal
  }

  private fun setAuthenticationContext(vararg authorities: GrantedAuthority) {
    val principal = UserProjectionBuilder.user().build()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, listOf(*authorities))
    getContext().authentication = authentication
  }

  private fun setAnonymousAuthenticationContext() {
    val authentication: Authentication =
        AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER"))
    getContext().authentication = authentication
  }

  @Nested
  @DisplayName("authorization for multiple inputs/results")
  inner class MultiAuth {

    @Test
    @DisplayName("is granted")
    fun verifyCheckAuthorizationMultipleResultsForGrantingQuery() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID()
      val identifier2 = randomUUID()
      val givenIdentifiers: Set<UUID> = setOf(identifier1, identifier2)
      val targetIdentifiers: Set<UUID> = setOf(identifier1, identifier2)

      val queryFunction: BiFunction<Set<UUID>, UserId, Set<UUID>> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.id) } returns targetIdentifiers

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isTrue
    }

    @Test
    @DisplayName("is denied for empty inputs")
    fun verifyCheckAuthorizationMultipleResultsForDenyingQueryEmptyInput() {
      val principal = setAuthenticationContext()
      val givenIdentifiers = emptySet<UUID>()

      val queryFunction: BiFunction<Set<UUID>, UserId, Set<UUID>> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.id) } returns emptySet()

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isFalse
    }

    @Test
    @DisplayName("is denied for mismatch between input and results")
    fun verifyCheckAuthorizationMultipleResultsForDenyingQueryNotContainsAll() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID()
      val identifier2 = randomUUID()
      val givenIdentifiers: Set<UUID> = setOf(identifier1, identifier2)
      val targetIdentifiers: Set<UUID> = setOf(identifier1)

      val queryFunction: BiFunction<Set<UUID>, UserId, Set<UUID>> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.id) } returns targetIdentifiers

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isFalse
    }
  }

  @Nested
  @DisplayName("authorization for single inputs/results")
  inner class SingleAuth {

    @Test
    @DisplayName("is granted")
    fun verifyCheckAuthorizationSingleResultForGrantingQuery() {
      val principal = setAuthenticationContext()
      val targetIdentifier = randomUUID()

      val queryFunction: BiFunction<UUID, UserId, Any?> = mockk(relaxed = true)
      every { queryFunction.apply(targetIdentifier, principal.id) } returns Object()

      assertThat(
              AuthorizationUtils.checkAuthorizationForSingleResult(queryFunction, targetIdentifier))
          .isTrue
    }

    @Test
    @DisplayName("is denied for null input")
    fun verifyCheckAuthorizationSingleResultForDenyingQueryNull() {
      val principal = setAuthenticationContext()

      val queryFunction: BiFunction<UUID?, UserId, Any?> = mockk(relaxed = true)
      every { queryFunction.apply(null, principal.id) } returns null

      assertThat(AuthorizationUtils.checkAuthorizationForSingleResult(queryFunction, null)).isFalse
    }

    @Test
    @DisplayName("is denied for not returning any result")
    fun verifyCheckAuthorizationSingleResultForDenyingQuery() {
      val principal = setAuthenticationContext()
      val targetIdentifier = randomUUID()

      val queryFunction: BiFunction<UUID, UserId, Any?> = mockk(relaxed = true)
      every { queryFunction.apply(targetIdentifier, principal.id) } returns null

      assertThat(
              AuthorizationUtils.checkAuthorizationForSingleResult(queryFunction, targetIdentifier))
          .isFalse
    }
  }

  @Nested
  @DisplayName("admin role")
  inner class AdminRole {

    @Test
    @DisplayName("is granted for user having role 'ADMIN'")
    fun verifyHasAdminRoleForAdmin() {
      setAuthenticationContext(SimpleGrantedAuthority(RoleConstants.ADMIN.roleName()))
      assertThat(AuthorizationUtils.hasRoleAdmin()).isTrue
    }

    @Test
    @DisplayName("is not granted for regular user")
    fun verifyHasNoAdminRoleForRegularUser() {
      setAuthenticationContext()
      assertThat(AuthorizationUtils.hasRoleAdmin()).isFalse
    }

    @Test
    @DisplayName("is not granted for missing authentication")
    fun verifyHasNoAdminRoleForNoAuthenticationContext() {
      assertThat(AuthorizationUtils.hasRoleAdmin()).isFalse
    }

    @Test
    @DisplayName("is not granted for unauthenticated user")
    fun verifyHasNoAdminRoleForAnonymousAuthenticationContext() {
      setAnonymousAuthenticationContext()
      assertThat(AuthorizationUtils.hasRoleAdmin()).isFalse
    }
  }
}
