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
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.DeleteUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.DeleteUserCommandHandler
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@CodeExample
@DisplayName("Verify authorization for deleting users")
class DeleteUserCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: DeleteUserCommandHandler

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `deletes a user by identifier if the executing user is ADMIN`(userType: UserTypeAccess) {
    eventStreamGenerator.submitUser("testUser").submitProfilePicture()

    checkAccessWith(userType) {
      cut.handle(DeleteUserCommand(UserId(eventStreamGenerator.getIdentifier("testUser"))))
    }
  }

  @Test
  fun `deleting a user is only successfull if restricted admin is authorized for users country`() {
    addUsersAndRestrictAdminAccessToOneCountry()
    AuthorizationTestUtils.authorizeWithUser(userMap[ADMIN]!!.left, userMap[ADMIN]!!.right)

    Assertions.assertThat(cut.handle(DeleteUserCommand(userIdOf("userInAL")))).isNotNull
    Assertions.assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(DeleteUserCommand(userIdOf("userInAD")))
    }
  }
}
