/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.INVESTOR
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.command.api.DeleteMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import java.time.LocalDate.now
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for DeleteMilestoneCommandHandler")
class DeleteMilestoneCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: DeleteMilestoneCommandHandler

  // the milestone reference is set inside submitMilestonesWithList()
  private val milestoneIdentifier by lazy { getIdentifier("milestoneListM0").asMilestoneId() }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator.setUserContext("userCsm").submitProjectCraftG2()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify delete investor milestone is authorized for`(userType: UserTypeAccess) {
    // Only the csm users can create project milestones
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitMilestonesWithList(
            date = now(), milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))

    checkAccessWith(userType) { cut.handle(buildDeleteMilestoneCommand()) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify delete project milestone is authorized for`(userType: UserTypeAccess) {
    // Only the csm users can create project milestones
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitMilestonesWithList(
            date = now(), milestones = listOf(SubmitMilestoneWithListDto(type = PROJECT)))

    checkAccessWith(userType) { cut.handle(buildDeleteMilestoneCommand()) }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify delete craft milestone created by a FM is authorized for`(userType: UserTypeAccess) {
    useOnlineListener()
    eventStreamGenerator
        .setUserContext("userCreator")
        .submitMilestonesWithList(
            date = now(),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = CRAFT, craft = getByReference("projectCraft"))))

    checkAccessWith(userType) { cut.handle(buildDeleteMilestoneCommand()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify delete milestone for non-existing identifier is denied for`(
      userType: UserTypeAccess
  ) = checkAccessWith(userType) { cut.handle(buildDeleteMilestoneCommand(MilestoneId())) }

  private fun buildDeleteMilestoneCommand(
      identifier: MilestoneId = milestoneIdentifier,
  ): DeleteMilestoneCommand = DeleteMilestoneCommand(identifier = identifier, version = 0)
}
