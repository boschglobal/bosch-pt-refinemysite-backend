/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneRescheduleService
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

open class MilestoneRescheduleServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: MilestoneRescheduleService

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator.setUserContext("userCsm").submitProjectCraftG2()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify validate reschedule milestone is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.validateMilestones(SearchMilestonesDto(projectIdentifier = projectIdentifier))
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify reschedule milestone is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.rescheduleMilestones(2, SearchMilestonesDto(projectIdentifier = projectIdentifier))
    }
  }
}
