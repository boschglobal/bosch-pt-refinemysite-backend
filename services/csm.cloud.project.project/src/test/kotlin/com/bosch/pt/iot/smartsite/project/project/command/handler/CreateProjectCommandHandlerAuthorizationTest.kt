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
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.CreateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.NB
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization of create project command handler")
class CreateProjectCommandHandlerAuthorizationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: CreateProjectCommandHandler

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  @DisplayName("create permission is granted to")
  fun verifyCreateProjectIsAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val newProjectIdentifier =
          cut.handle(
              CreateProjectCommand(
                  identifier = ProjectId(),
                  client = "Test Client",
                  description = "Descriptions",
                  start = now(),
                  end = now().plusDays(5),
                  projectNumber = "Project Number",
                  title = "Project Title",
                  category = NB,
                  address =
                      ProjectAddressVo(
                          street = "Musterstrasse",
                          houseNumber = "1",
                          zipCode = "12345",
                          city = "Fake Town")))
      assertThat(newProjectIdentifier).isNotNull()
    }
  }
}
