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
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.LockUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UnlockUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.LockUnlockUserCommandHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@CodeExample
@DisplayName("Verify authorization for locking / unlocking users")
class LockUnlockUserCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: LockUnlockUserCommandHandler

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `locking a user is successful`(userType: UserTypeAccess) {
    eventStreamGenerator.submitUser("testUser")
    checkAccessWith(userType) {
      cut.handle(LockUserCommand(UserId(eventStreamGenerator.getIdentifier("testUser"))))
    }
  }

  @Test
  fun `locking a user is only successfull if restricted admin is authorized for users country`() {
    addUsersAndRestrictAdminAccessToOneCountry()
    authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)

    assertThat(cut.handle(LockUserCommand(userIdOf("userInAL")))).isNotNull
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(LockUserCommand(userIdOf("userInAD")))
    }
  }

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `unlocking a locked user is successful`(userType: UserTypeAccess) {
    eventStreamGenerator.submitUser("testUser") { it.locked = true }

    checkAccessWith(userType) {
      cut.handle(UnlockUserCommand(UserId(eventStreamGenerator.getIdentifier("testUser"))))
    }
  }

  @Test
  fun `unlocking a locked user is only successfull if restricted admin is authorized for users country`() {
    addUsersAndRestrictAdminAccessToOneCountry(locked = true)
    authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)

    assertThat(cut.handle(UnlockUserCommand(userIdOf("userInAL")))).isNotNull
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(UnlockUserCommand(userIdOf("userInAD")))
    }
  }
}
