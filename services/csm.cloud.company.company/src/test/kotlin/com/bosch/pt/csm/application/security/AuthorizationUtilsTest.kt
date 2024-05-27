/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.user.user.constants.RoleConstants
import com.bosch.pt.csm.user.user.model.UserBuilder
import com.bosch.pt.csm.user.user.query.UserProjection
import com.google.common.collect.Sets
import io.mockk.every
import io.mockk.mockk
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
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/** Test the common authorization utils in [AuthorizationUtils]. */
@SmartSiteMockKTest
@DisplayName("Verify")
class AuthorizationUtilsTest {

  /** Clean up after tests. */
  @AfterEach
  fun cleanup() {
    SecurityContextHolder.getContext().authentication = null
  }

  private fun setAuthenticationContext(): UserProjection {
    val principal = UserBuilder.user().build()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    SecurityContextHolder.getContext().authentication = authentication
    return principal
  }

  private fun setAuthenticationContext(vararg authorities: GrantedAuthority) {
    val principal = UserBuilder.user().build()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, listOf(*authorities))
    SecurityContextHolder.getContext().authentication = authentication
  }

  private fun setAnonymousAuthenticationContext() {
    val authentication: Authentication =
        AnonymousAuthenticationToken("n/a", "n/a", AuthorityUtils.createAuthorityList("USER"))
    SecurityContextHolder.getContext().authentication = authentication
  }

  @Nested
  @DisplayName("authorization for multiple inputs/results")
  inner class MultiAuth {

    @Test
    @DisplayName("is granted")
    fun verifyCheckAuthorizationMultipleResultsForGrantingQuery() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID().asUserId()
      val identifier2 = randomUUID().asUserId()
      val givenIdentifiers: Set<UserId> = setOf(identifier1, identifier2)
      val targetIdentifiers: Set<UserId> = setOf(identifier1, identifier2)

      val queryFunction: BiFunction<Set<UserId>, UserId?, Set<UserId>> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.id) } returns targetIdentifiers

      assertThat(
              AuthorizationUtils.checkAuthorizationForMultipleResults(
                  queryFunction, givenIdentifiers))
          .isTrue
    }

    @Test
    @DisplayName("is denied for empty inputs")
    fun verifyCheckAuthorizationMultipleResultsForDenyingQueryEmptyInput() {
      val principal = setAuthenticationContext()
      val givenIdentifiers = emptySet<UserId>()

      val queryFunction: BiFunction<Set<UserId>, UserId?, Set<UserId>> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.id) } returns emptySet()

      assertThat(
              AuthorizationUtils.checkAuthorizationForMultipleResults(
                  queryFunction, givenIdentifiers))
          .isFalse
    }

    @Test
    @DisplayName("is denied for mismatch between input and results")
    fun verifyCheckAuthorizationMultipleResultsForDenyingQueryNotContainsAll() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID().asUserId()
      val identifier2 = randomUUID().asUserId()
      val givenIdentifiers: Set<UserId> = Sets.newHashSet(identifier1, identifier2)
      val targetIdentifiers: Set<UserId> = Sets.newHashSet(identifier1)

      val queryFunction: BiFunction<Set<UserId>, UserId?, Set<UserId>> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.id) } returns targetIdentifiers

      assertThat(
              AuthorizationUtils.checkAuthorizationForMultipleResults(
                  queryFunction, givenIdentifiers))
          .isFalse
    }
  }

  @Nested
  @DisplayName("authorization for single inputs/results")
  inner class SingleAuth {

    @Test
    @DisplayName("is granted")
    fun verifyCheckAuthorizationSingleResultForGrantingQuery() {
      val principal = setAuthenticationContext()
      val targetIdentifier = randomUUID().asUserId()

      val queryFunction: BiFunction<UserId, UserId?, Any?> = mockk(relaxed = true)
      every { queryFunction.apply(targetIdentifier, principal.id) } returns Object()

      assertThat(
              AuthorizationUtils.checkAuthorizationForSingleResult(queryFunction, targetIdentifier))
          .isTrue
    }

    @Test
    @DisplayName("is denied for null input")
    fun verifyCheckAuthorizationSingleResultForDenyingQueryNull() {
      val principal = setAuthenticationContext()

      val queryFunction: BiFunction<UserId?, UserId?, Any?> = mockk(relaxed = true)
      every { queryFunction.apply(null, principal.id) } returns null

      assertThat(AuthorizationUtils.checkAuthorizationForSingleResult(queryFunction, null)).isFalse
    }

    @Test
    @DisplayName("is denied for not returning any result")
    fun verifyCheckAuthorizationSingleResultForDenyingQuery() {
      val principal = setAuthenticationContext()
      val targetIdentifier = randomUUID().asUserId()

      val queryFunction: BiFunction<UserId, UserId?, Any?> = mockk(relaxed = true)
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
