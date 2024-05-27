/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for CreateMilestoneCommandHandler")
class CreateMilestoneCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: CreateMilestoneCommandHandler

  private val projectCraftIdentifier by lazy { getIdentifier("projectCraft").asProjectCraftId() }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator.setUserContext("userCsm").submitProjectCraftG2()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify create investor milestone is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(buildCreateMilestoneCommand(type = MilestoneTypeEnum.INVESTOR))
      }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify create project milestone is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(buildCreateMilestoneCommand(type = MilestoneTypeEnum.PROJECT))
      }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify create craft milestone is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            buildCreateMilestoneCommand(
                type = MilestoneTypeEnum.CRAFT, craftIdentifier = projectCraftIdentifier))
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify create milestone for non-existing project is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            buildCreateMilestoneCommand(
                type = MilestoneTypeEnum.INVESTOR, projectIdentifier = ProjectId()))
      }

  private fun buildCreateMilestoneCommand(
      type: MilestoneTypeEnum,
      projectIdentifier: ProjectId = project.identifier,
      craftIdentifier: ProjectCraftId? = null
  ): CreateMilestoneCommand =
      CreateMilestoneCommand(
          identifier = MilestoneId(),
          projectRef = projectIdentifier,
          name = "Test",
          type = type,
          date = LocalDate.now(),
          header = false,
          craftRef = craftIdentifier,
          position = 0)
}
