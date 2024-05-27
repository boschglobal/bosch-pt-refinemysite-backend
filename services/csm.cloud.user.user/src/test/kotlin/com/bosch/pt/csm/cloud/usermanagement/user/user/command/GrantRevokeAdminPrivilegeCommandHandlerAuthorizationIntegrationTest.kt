/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.GrantAdminPrivilegeCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.RevokeAdminPrivilegeCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.GrantRevokeAdminPrivilegeCommandHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@CodeExample
@DisplayName("Verify authorization for granting / revoking admin permission")
class GrantRevokeAdminPrivilegeCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: GrantRevokeAdminPrivilegeCommandHandler

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `granting admin privilege to a user is successful`(userType: UserTypeAccess) {
    eventStreamGenerator.submitUser("testUser")
    checkAccessWith(userType) {
      cut.handle(GrantAdminPrivilegeCommand(UserId(eventStreamGenerator.getIdentifier("testUser"))))
    }
  }

  @Test
  fun `granting admin privilege to a user is only successfull if restricted admin is authorized for users country`() {
    addUsersAndRestrictAdminAccessToOneCountry()
    AuthorizationTestUtils.authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)

    assertThat(cut.handle(GrantAdminPrivilegeCommand(userIdOf("userInAL")))).isNotNull
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(GrantAdminPrivilegeCommand(userIdOf("userInAD")))
    }
  }

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `revoking admin privilege from a user is successful`(userType: UserTypeAccess) {
    eventStreamGenerator.submitUser("testUser")
    checkAccessWith(userType) {
      cut.handle(
          RevokeAdminPrivilegeCommand(UserId(eventStreamGenerator.getIdentifier("testUser"))))
    }
  }

  @Test
  fun `revoking admin privilege from a user is only successfull if restricted admin is authorized for users country`() {
    addUsersAndRestrictAdminAccessToOneCountry(admin = true)
    AuthorizationTestUtils.authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)

    assertThat(cut.handle(RevokeAdminPrivilegeCommand(userIdOf("userInAL")))).isNotNull
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(RevokeAdminPrivilegeCommand(userIdOf("userInAD")))
    }
  }
}
