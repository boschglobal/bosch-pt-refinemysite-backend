/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateProjectCraftCommandHandlerAuthorizationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: CreateProjectCraftCommandHandler

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2(asReference = "projectCraft")
        .submitProjectCraftList(asReference = "projectCraftList")
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify create project craft authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(buildCreateProjectCraftCommand()) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify create project craft for non-existing project is denied for`(
      userType: UserTypeAccess
  ) = checkAccessWith(userType) { cut.handle(buildCreateProjectCraftCommand(ProjectId())) }

  private fun buildCreateProjectCraftCommand(projectIdentifier: ProjectId = project.identifier) =
      CreateProjectCraftCommand(
          projectIdentifier = projectIdentifier,
          identifier = ProjectCraftId(),
          name = "ProjectCraft",
          color = "#000000",
          projectCraftListVersion = 0L,
          position = 1)
}
