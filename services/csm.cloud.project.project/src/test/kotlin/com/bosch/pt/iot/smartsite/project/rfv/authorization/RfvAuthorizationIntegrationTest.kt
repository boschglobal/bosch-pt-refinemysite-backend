/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.authorization

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.UpdateRfvDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class RfvAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: RfvService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find all is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAll(project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find all for non-existing project is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAll(ProjectId()) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify resolve project rfvs is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.resolveProjectRfvs(project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify resolve project rfvs for non-existing project is denied for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.resolveProjectRfvs(ProjectId()) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update rfvs is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.update(UpdateRfvDto(project.identifier, DayCardReasonEnum.CUSTOM1, true, "Custom Rfv"))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify update rfvs for non-existing project is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.update(UpdateRfvDto(ProjectId(), DayCardReasonEnum.CUSTOM1, true, "Custom Rfv"))
    }
  }
}
