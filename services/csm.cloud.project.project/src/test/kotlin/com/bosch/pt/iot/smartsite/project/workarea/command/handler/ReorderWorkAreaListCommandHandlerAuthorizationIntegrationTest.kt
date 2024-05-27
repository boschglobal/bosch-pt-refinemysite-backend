/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.workarea.command.api.ReorderWorkAreaListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.list.ReorderWorkAreaListCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for ReorderWorkAreaListCommandHandler")
class ReorderWorkAreaListCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ReorderWorkAreaListCommandHandler

  private val workAreaIdentifier by lazy { getIdentifier("workArea1").asWorkAreaId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitWorkArea("workArea1")
        .submitWorkArea("workArea2")
        .submitWorkArea("workArea3")
        .submitWorkAreaList {
          it.workAreas =
              listOf(
                  getByReference("workArea1"),
                  getByReference("workArea2"),
                  getByReference("workArea3"))
        }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update workArea is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(buildReorderWorkAreaCommand(workAreaRef = workAreaIdentifier, position = 2))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify update workArea for non-existing identifier is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(buildReorderWorkAreaCommand(workAreaRef = WorkAreaId(), position = 3))
      }

  private fun buildReorderWorkAreaCommand(
      workAreaRef: WorkAreaId = workAreaIdentifier,
      position: Int,
      version: Long = 0
  ): ReorderWorkAreaListCommand =
      ReorderWorkAreaListCommand(workAreaRef = workAreaRef, version = version, position = position)
}
