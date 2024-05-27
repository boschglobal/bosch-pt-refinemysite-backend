/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

open class MilestoneQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: MilestoneQueryService

  private val milestoneIdentifier by lazy { getIdentifier("milestone") }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator.setUserContext("userCsm").submitProjectCraftG2()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify view milestone is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator
        // Only the csm users can create project milestones
        .setUserContext("userCsm")
        .submitMilestone { milestone ->
          milestone.type = MilestoneTypeEnumAvro.PROJECT
          milestone.header = false
        }

    checkAccessWith(userType) { cut.find(milestoneIdentifier.asMilestoneId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify view milestone for non-existing identifier is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.find(MilestoneId()) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify view milestones of a project is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator.setUserContext("userCreator").submitMilestone("m1").submitMilestone("m2")

    checkAccessWith(userType) {
      cut.findBatch(
          setOf(getIdentifier("m1").asMilestoneId(), getIdentifier("m2").asMilestoneId()),
          project.identifier)
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify view milestones of a project for non-existing identifier is denied for`(
      userType: UserTypeAccess
  ) {
    eventStreamGenerator.setUserContext("userCreator").submitMilestone("m1")

    checkAccessWith(userType) {
      cut.findBatch(setOf(getIdentifier("m1").asMilestoneId(), MilestoneId()), project.identifier)
    }
  }
}
