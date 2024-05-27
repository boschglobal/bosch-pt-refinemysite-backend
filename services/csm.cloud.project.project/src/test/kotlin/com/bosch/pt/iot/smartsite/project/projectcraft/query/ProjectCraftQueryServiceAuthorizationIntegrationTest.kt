/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ProjectCraftQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectCraftQueryService

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2(asReference = "projectCraft")
        .submitProjectCraftList(asReference = "projectCraftList")
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find one project craft by identifier is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.findOneByIdentifier(getIdentifier("projectCraft").asProjectCraftId())
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find one project craft with non-existing identifier is denied for`(
      userType: UserTypeAccess
  ) = checkAccessWith(userType) { cut.findOneByIdentifier(ProjectCraftId()) }
}
