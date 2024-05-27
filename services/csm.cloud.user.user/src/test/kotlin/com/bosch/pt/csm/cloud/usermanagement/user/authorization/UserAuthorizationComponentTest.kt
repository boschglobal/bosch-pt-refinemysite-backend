/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.authorization

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
import java.time.LocalDate
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

/** Verify that the [UserAuthorizationComponent] works as expected. */
@SmartSiteMockKTest
class UserAuthorizationComponentTest {

  private lateinit var cut: UserAuthorizationComponent

  private val systemUserIdentifier = UserId()

  /** Initialise class under test. */
  @BeforeEach
  fun init() {
    cut = UserAuthorizationComponent(systemUserIdentifier)
  }

  /** Clean up after tests. */
  @AfterEach
  fun cleanup() {
    SecurityContextHolder.getContext().authentication = null
  }

  /** Verifies that a [User] can update their own [User] object. */
  @Test
  fun verifyUserCanWriteOwnUser() {
    val userIdentifier = UserId()
    val principal = defaultUser().apply { identifier = userIdentifier }
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
    SecurityContextHolder.getContext().authentication = authentication
    assertThat(cut.isCurrentUser(userIdentifier)).isTrue
  }

  /** Verifies that a [User] cannot update another [User] object. */
  @Test
  fun verifyUserCannotWriteOtherUser() {
    val userIdentifier = UserId()
    val principal = defaultUser()
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
    SecurityContextHolder.getContext().authentication = authentication
    assertThat(cut.isCurrentUser(userIdentifier)).isFalse
  }

  @Test
  fun verifyIsCurrentUserSystemUser() {
    val principal =
        User(
            systemUserIdentifier,
            "SYSTEM",
            GenderEnum.FEMALE,
            "System",
            "User",
            "system@example.com",
            Locale.ENGLISH,
            IsoCountryCodeEnum.US,
            LocalDate.now())
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
    SecurityContextHolder.getContext().authentication = authentication
    assertThat(cut.isCurrentUserSystemUser()).isTrue()
  }
}
