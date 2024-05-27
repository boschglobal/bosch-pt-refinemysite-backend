/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils.checkAuthorizationForMultipleResults
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils.checkAuthorizationForSingleResult
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils.hasRoleAdmin
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.function.BiFunction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Lists
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

@SmartSiteMockKTest
@DisplayName("Verify")
class AuthorizationUtilsTest {

  @AfterEach
  fun cleanup() {
    SecurityContextHolder.getContext().authentication = null
  }

  @Nested
  @DisplayName("authorization for multiple inputs/results")
  inner class MultiAuth {

    @Test
    fun `is granted`() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID()
      val identifier2 = randomUUID()
      val givenIdentifiers = setOf(identifier1, identifier2)
      val targetIdentifiers = setOf(identifier1, identifier2)

      val queryFunction: BiFunction<Set<UUID>, UUID?, Set<UUID>> = mockk()
      every { queryFunction.apply(givenIdentifiers, principal.getIdentifierUuid()) } returns
          targetIdentifiers

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isTrue
    }

    @Test
    fun `is denied for empty inputs`() {
      val principal = setAuthenticationContext()
      val givenIdentifiers = emptySet<UUID>()

      val queryFunction: BiFunction<Set<UUID>, UUID?, Set<UUID>> = mockk()
      every { queryFunction.apply(givenIdentifiers, principal.getIdentifierUuid()) } returns emptySet()

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isFalse
    }

    @Test
    fun `is denied for mismatch between input and results`() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID()
      val identifier2 = randomUUID()
      val givenIdentifiers = setOf(identifier1, identifier2)
      val targetIdentifiers = setOf(identifier1)

      val queryFunction: BiFunction<Set<UUID>, UUID?, Set<UUID>> = mockk()
      every { queryFunction.apply(givenIdentifiers, principal.getIdentifierUuid()) } returns
          targetIdentifiers

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isFalse
    }
  }

  @Nested
  @DisplayName("authorization for single inputs/results")
  inner class SingleAuth {

    @Test
    fun `is granted`() {
      val principal = setAuthenticationContext()
      val targetIdentifier = randomUUID()

      val queryFunction: BiFunction<UUID, UUID?, Any> = mockk()
      every { queryFunction.apply(targetIdentifier, principal.getIdentifierUuid()) } returns Any()

      assertThat(checkAuthorizationForSingleResult(queryFunction, targetIdentifier)).isTrue
    }

    @Test
    fun `is denied for null input`() {
      val principal = setAuthenticationContext()

      val queryFunction: BiFunction<UUID?, UUID?, Any?> = mockk()
      every { queryFunction.apply(null, principal.getIdentifierUuid()) } returns null

      assertThat(checkAuthorizationForSingleResult(queryFunction, null)).isFalse
    }

    @Test
    @DisplayName("is denied for not returning any result")
    fun verifyCheckAuthorizationSingleResultForDenyingQuery() {
      val principal = setAuthenticationContext()
      val targetIdentifier = randomUUID()

      val queryFunction: BiFunction<UUID, UUID?, Any?> = mockk()
      every { queryFunction.apply(targetIdentifier, principal.getIdentifierUuid()) } returns null

      assertThat(checkAuthorizationForSingleResult(queryFunction, targetIdentifier)).isFalse
    }
  }

  @Nested
  @DisplayName("admin role")
  inner class AdminRole {

    @Test
    fun `is granted for user having role 'ADMIN'`() {
      setAuthenticationContext(SimpleGrantedAuthority(ADMIN.roleName()))
      assertThat(hasRoleAdmin()).isTrue
    }

    @Test
    fun `is not granted for regular user`() {
      setAuthenticationContext()
      assertThat(hasRoleAdmin()).isFalse
    }

    @Test
    fun `is not granted for missing authentication`() {
      assertThat(hasRoleAdmin()).isFalse
    }

    @Test
    fun `is not granted for unauthenticated user`() {
      setAnonymousAuthenticationContext()
      assertThat(hasRoleAdmin()).isFalse
    }
  }

  private fun setAuthenticationContext(): User {
    val principal = defaultUser()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
    SecurityContextHolder.getContext().authentication = authentication
    return principal
  }

  private fun setAuthenticationContext(vararg authorities: GrantedAuthority) {
    val principal = defaultUser()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(
            principal, principal.password, Lists.newArrayList(*authorities))
    SecurityContextHolder.getContext().authentication = authentication
  }

  private fun setAnonymousAuthenticationContext() {
    val authentication: Authentication =
        AnonymousAuthenticationToken("n/a", "n/a", AuthorityUtils.createAuthorityList("USER"))
    SecurityContextHolder.getContext().authentication = authentication
  }
}
