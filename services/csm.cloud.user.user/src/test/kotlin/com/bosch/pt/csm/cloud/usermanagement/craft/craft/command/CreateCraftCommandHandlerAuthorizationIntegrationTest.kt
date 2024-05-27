/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.command

import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.api.CreateCraftCommand
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.handler.CreateCraftCommandHandler
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify authorization for adding crafts")
class CreateCraftCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var createCraftCommandHandler: CreateCraftCommandHandler

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `verify admin is authorized to create craft`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      createCraftCommandHandler.handle(
          CreateCraftCommand(CraftId.random(), defaultName = "Electricity"))
    }
  }
}
