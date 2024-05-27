/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.INVESTOR
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.command.api.UpdateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import java.time.LocalDate.now
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for UpdateMilestoneCommandHandler")
class UpdateMilestoneCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: UpdateMilestoneCommandHandler

  // the milestone reference is set inside submitMilestonesWithList()
  private val milestoneIdentifier by lazy { getIdentifier("milestoneListM0").asMilestoneId() }

  private val projectCraftIdentifier by lazy { getIdentifier("projectCraft").asProjectCraftId() }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator.setUserContext("userCsm").submitProjectCraftG2()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update investor milestone is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator
        // Only the csm users can create project milestones
        .setUserContext("userCsm")
        .submitMilestonesWithList(
            date = now(), milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))

    checkAccessWith(userType) {
      cut.handle(buildUpdateMilestoneCommand(type = MilestoneTypeEnum.INVESTOR, version = 0))
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update project milestone is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator
        // Only the csm users can create project milestones
        .setUserContext("userCsm")
        .submitMilestonesWithList(
            date = now(), milestones = listOf(SubmitMilestoneWithListDto(type = PROJECT)))

    checkAccessWith(userType) {
      cut.handle(buildUpdateMilestoneCommand(type = MilestoneTypeEnum.PROJECT, version = 0))
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify update craft milestone created by an FM is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator
        .setUserContext("userCreator")
        .submitMilestonesWithList(
            date = now(),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = CRAFT,
                        craft =
                            EventStreamGeneratorStaticExtensions.getByReference("projectCraft"))))

    checkAccessWith(userType) {
      cut.handle(
          buildUpdateMilestoneCommand(
              type = MilestoneTypeEnum.CRAFT,
              craftIdentifier = projectCraftIdentifier,
              version = 0))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify update milestone for non-existing identifier is denied for`(
      userType: UserTypeAccess
  ) =
      checkAccessWith(userType) {
        cut.handle(
            buildUpdateMilestoneCommand(
                identifier = MilestoneId(),
                type = MilestoneTypeEnum.CRAFT,
                craftIdentifier = projectCraftIdentifier,
                version = 0))
      }

  private fun buildUpdateMilestoneCommand(
      identifier: MilestoneId = milestoneIdentifier,
      version: Long,
      type: MilestoneTypeEnum,
      craftIdentifier: ProjectCraftId? = null
  ): UpdateMilestoneCommand =
      UpdateMilestoneCommand(
          identifier = identifier,
          version = version,
          name = "Test",
          type = type,
          date = now().plusDays(1),
          header = false,
          craftRef = craftIdentifier)
}
