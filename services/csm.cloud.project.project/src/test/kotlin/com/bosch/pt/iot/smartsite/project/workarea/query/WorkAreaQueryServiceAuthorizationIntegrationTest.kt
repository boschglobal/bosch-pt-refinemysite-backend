/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class WorkAreaQueryServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: WorkAreaQueryService

  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }

  @BeforeEach
  fun init() {
    // Only the csm user can add work areas to a project
    eventStreamGenerator.setUserContext("userCsm").submitWorkArea()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find one work area is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findOneWithDetailsByIdentifier(workArea.identifier) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find one work area is denied for non existing work area`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findOneWithDetailsByIdentifier(WorkAreaId()) }
}
