/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.checkAuthorizationForMultipleResults
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.hasRoleAdmin
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants.ADMIN
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.function.BiFunction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Lists
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@SmartSiteMockKTest
class AuthorizationUtilsTest {

  /** Clean up after tests. */
  @AfterEach
  fun cleanup() {
    SecurityContextHolder.getContext().authentication = null
  }

  @Nested
  inner class `Authorization for multiple inputs and results` {

    @Test
    fun `is granted`() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID()
      val identifier2 = randomUUID()
      val givenIdentifiers: Set<UUID> = setOf(identifier1, identifier2)
      val targetIdentifiers: Set<UUID> = setOf(identifier1, identifier2)

      val queryFunction: BiFunction<Set<UUID>, UUID, Set<UUID>?> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.identifier!!) } returns
          targetIdentifiers

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isTrue
    }

    @Test
    fun `is denied for empty inputs`() {
      val principal = setAuthenticationContext()
      val givenIdentifiers = emptySet<UUID>()

      val queryFunction: BiFunction<Set<UUID>, UUID, Set<UUID>?> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.identifier!!) } returns emptySet()

      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isFalse
    }

    @Test
    fun `is denied for mismatch between input and results`() {
      val principal = setAuthenticationContext()
      val identifier1 = randomUUID()
      val identifier2 = randomUUID()
      val givenIdentifiers: Set<UUID> = setOf(identifier1, identifier2)
      val targetIdentifiers: Set<UUID> = setOf(identifier1)

      val queryFunction: BiFunction<Set<UUID>, UUID, Set<UUID>?> = mockk(relaxed = true)
      every { queryFunction.apply(givenIdentifiers, principal.identifier!!) } returns
          targetIdentifiers
      assertThat(checkAuthorizationForMultipleResults(queryFunction, givenIdentifiers)).isFalse
    }
  }

  @Nested
  inner class `Admin role` {
    @Test
    fun `is granted for user having role ADMIN`() {
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

  @Nested
  inner class `Run block of code as authenticated user` {

    @Test
    fun `without any previous authentication context`() {
      val principalTestUser = user().build()

      doWithAuthenticatedUser(principalTestUser) {
        assertThat(SecurityContextHelper.getInstance().getCurrentUser())
            .isEqualTo(principalTestUser)
      }
      assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `with one existing previous authentication context`() {
      val principalTestUser = user().build()
      setAnonymousAuthenticationContext()

      doWithAuthenticatedUser(principalTestUser) {
        assertThat(SecurityContextHelper.getInstance().getCurrentUser())
            .isEqualTo(principalTestUser)
      }
      assertThat(SecurityContextHolder.getContext().authentication)
          .isEqualTo(AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER")))
    }

    @Test
    fun `with nested authentication block`() {
      val firstUser = user().build()
      val secondUser = user().asAdmin().build()
      setAnonymousAuthenticationContext()

      doWithAuthenticatedUser(firstUser) {
        assertThat(SecurityContextHelper.getInstance().getCurrentUser()).isEqualTo(firstUser)
        doWithAuthenticatedUser(secondUser) {
          assertThat(SecurityContextHelper.getInstance().getCurrentUser()).isEqualTo(secondUser)
        }
        assertThat(SecurityContextHelper.getInstance().getCurrentUser()).isEqualTo(firstUser)
      }
      assertThat(SecurityContextHolder.getContext().authentication)
          .isEqualTo(AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER")))
    }
  }

  private fun setAuthenticationContext(): User {
    val principal = user().build()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    SecurityContextHolder.getContext().authentication = authentication
    return principal
  }

  private fun setAuthenticationContext(vararg authorities: GrantedAuthority) {
    val principal = user().build()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(
            principal, principal.password, Lists.newArrayList(*authorities))

    SecurityContextHolder.getContext().authentication = authentication
  }

  private fun setAnonymousAuthenticationContext() {
    val authentication: Authentication =
        AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER"))

    SecurityContextHolder.getContext().authentication = authentication
  }
}
