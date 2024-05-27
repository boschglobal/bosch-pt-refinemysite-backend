/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.workarea.command.api.DeleteWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for DeleteWorkAreaCommandHandler")
class DeleteWorkAreaCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: DeleteWorkAreaCommandHandler

  private val workAreaId by lazy { getIdentifier("workArea").asWorkAreaId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("userCsm")
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify delete workArea is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator.setUserContext("userCsm").submitWorkArea().submitWorkAreaList()

    checkAccessWith(userType) { cut.handle(buildDeleteWorkAreaCommand()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify delete workArea for non-existing identifier is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(buildDeleteWorkAreaCommand(WorkAreaId())) }

  private fun buildDeleteWorkAreaCommand(
      identifier: WorkAreaId = workAreaId,
  ): DeleteWorkAreaCommand = DeleteWorkAreaCommand(identifier = identifier, version = 0)
}
