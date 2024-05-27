/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.GB
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.PhoneNumberCommandDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.UpdateUserCommandHandler
import java.util.Locale.GERMANY
import java.util.stream.Stream
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@CodeExample
@DisplayName("Verify authorization for updating user details")
class UpdateUserCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: UpdateUserCommandHandler

  @ParameterizedTest
  @MethodSource("onlyUserItself")
  fun `updating a user by himself`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(
          UpdateUserCommand(
              user.identifier,
              user.version,
              user.gender,
              user.firstName,
              "Musterfrau",
              user.position,
              user.crafts.map { it.identifier },
              user.phonenumbers
                  .map {
                    PhoneNumberCommandDto(
                        countryCode = it.countryCode,
                        callNumber = it.callNumber,
                        phoneNumberType = it.phoneNumberType,
                    )
                  }
                  .toSet(),
              GERMANY,
              GB))
    }
  }

  companion object {

    @JvmStatic
    fun onlyUserItself(): Stream<UserTypeAccess> {
      return createGrantedGroup(userTypes, setOf(USER))
    }
  }
}
