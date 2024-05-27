/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithStandardUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecurityContextHelperTest {

  /** Verify user has given role. */
  @Test
  fun hasRole() {
    authorizeWithAdministrativeUser(defaultUser())
    assertThat(SecurityContextHelper.hasRole("ADMIN")).isTrue
  }

  /** Verify user has no role without authentication. */
  @Test
  fun hasRoleNoAuthentication() {
    authorizeWithStandardUser(defaultUser())
    assertThat(SecurityContextHelper.hasRole("ADMIN")).isFalse
  }

  /** Verify role matching with role prefix given. */
  @Test
  fun hasRoleWithPrefix() {
    authorizeWithAdministrativeUser(defaultUser())
    assertThat(SecurityContextHelper.hasRole("ROLE_ADMIN")).isTrue
  }

  /** Verify user does not have given role. */
  @Test
  fun hasNoRole() {
    authorizeWithStandardUser(defaultUser())
    assertThat(SecurityContextHelper.hasRole("DUMMY")).isFalse
  }

  /** Verify that user has any of the given roles. */
  @Test
  fun hasAnyRole() {
    authorizeWithStandardUser(defaultUser())
    assertThat(SecurityContextHelper.hasAnyRole("USER", "ADMIN")).isTrue
  }

  /** Verify expected current user is returned. */
  @Test
  fun getCurrentUser() {
    defaultUser().apply {
      authorizeWithStandardUser(this)
      assertThat(SecurityContextHelper.getCurrentUser()).isNotNull.isEqualTo(this)
    }
  }
}
