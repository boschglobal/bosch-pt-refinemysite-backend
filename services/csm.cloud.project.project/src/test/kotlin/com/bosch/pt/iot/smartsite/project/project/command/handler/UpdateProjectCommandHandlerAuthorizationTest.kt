/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.handler

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.command.api.UpdateProjectCommand
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization of update project command handler")
class UpdateProjectCommandHandlerAuthorizationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: UpdateProjectCommandHandler

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  @DisplayName("update permission is granted to")
  fun verifyUpdateProjectIsAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(
          UpdateProjectCommand(
              identifier = project.identifier,
              version = project.version,
              client = project.client,
              description = project.description,
              start = project.start,
              end = project.end.plusDays(5),
              projectNumber = project.projectNumber,
              title = project.title,
              category = project.category,
              address =
                  ProjectAddressVo(
                      street = project.projectAddress!!.street,
                      houseNumber = project.projectAddress!!.houseNumber,
                      zipCode = project.projectAddress!!.zipCode,
                      city = project.projectAddress!!.city)))
    }
  }
}
