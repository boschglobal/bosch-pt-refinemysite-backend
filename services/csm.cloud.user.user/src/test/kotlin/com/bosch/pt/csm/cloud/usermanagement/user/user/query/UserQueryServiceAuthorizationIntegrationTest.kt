/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.query

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@CodeExample
@DisplayName("Verify authorization for querying user data")
class UserQueryServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: UserQueryService

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `returns users if the executing user is ADMIN`(userType: UserTypeAccess) {
    checkAccessWith(userType) { assertThat(cut.findAllUsers(PageRequest.of(0, 5))).isNotNull() }
  }

  @Test
  fun `returns users of authorized countries if ADMIN and has restrictions`() {
    addUsersAndRestrictAdminAccessToOneCountry()
    authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)
    assertThat(cut.findAllUsers(PageRequest.of(0, 5))).hasSize(1)
  }

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `returns user suggestions if the executing user is ADMIN`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.suggestUsersByTerm(user.firstName!!, PageRequest.of(0, 5))).isNotNull()
    }
  }

  @Test
  fun `returns user suggestions for authorized countries if ADMIN is restricted`() {
    addUsersAndRestrictAdminAccessToOneCountry()
    authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)
    assertThat(cut.suggestUsersByTerm("findMe", PageRequest.of(0, 5))).hasSize(1)
  }
}
