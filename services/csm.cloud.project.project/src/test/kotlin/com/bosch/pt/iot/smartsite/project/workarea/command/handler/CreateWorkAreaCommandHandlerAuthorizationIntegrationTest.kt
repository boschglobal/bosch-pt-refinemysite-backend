/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for CreateWorkAreaCommandHandler")
class CreateWorkAreaCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: CreateWorkAreaCommandHandler

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitWorkArea("testWorkArea")
        .submitWorkAreaList { it.workAreas = listOf(getByReference("testWorkArea")) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify create workArea is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(buildCreateWorkAreaCommand()) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify create workArea for non-existing project is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(buildCreateWorkAreaCommand(projectIdentifier = ProjectId()))
      }

  private fun buildCreateWorkAreaCommand(
      projectIdentifier: ProjectId = project.identifier,
  ): CreateWorkAreaCommand =
      CreateWorkAreaCommand(
          identifier = WorkAreaId(),
          projectRef = projectIdentifier,
          name = "WorkAreaName",
          position = 1,
          0)
}
