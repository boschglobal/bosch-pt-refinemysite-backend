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
import com.bosch.pt.iot.smartsite.project.workarea.command.api.UpdateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for UpdateWorkAreaCommandHandler")
class UpdateWorkAreaCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: UpdateWorkAreaCommandHandler

  private val workAreaIdentifier by lazy { getIdentifier("workArea").asWorkAreaId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("userCsm")
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update workArea is authorized for`(userType: UserTypeAccess) {
    eventStreamGenerator.setUserContext("userCsm").submitWorkArea().submitWorkAreaList()

    checkAccessWith(userType) {
      cut.handle(buildUpdateWorkAreaCommand(identifier = workAreaIdentifier, "newName"))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify update workArea for non-existing identifier is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(buildUpdateWorkAreaCommand(identifier = WorkAreaId(), "new Name"))
      }

  private fun buildUpdateWorkAreaCommand(
      identifier: WorkAreaId = workAreaIdentifier,
      name: String
  ): UpdateWorkAreaCommand =
      UpdateWorkAreaCommand(identifier = identifier, version = 0, name = name)
}
