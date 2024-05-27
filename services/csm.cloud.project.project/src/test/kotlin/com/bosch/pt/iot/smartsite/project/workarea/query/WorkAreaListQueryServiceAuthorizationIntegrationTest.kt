/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class WorkAreaListQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: WorkAreaListQueryService

  @BeforeEach
  fun init() {
    // Only the csm user can add work areas to a project
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitWorkArea(asReference = "workAreaOne")
        .submitWorkArea(asReference = "workAreaTwo")
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find a work area list is authorized for`(userType: UserTypeAccess) {
    createWorkAreaList()

    checkAccessWith(userType) { cut.findOneWithDetailsByProjectIdentifier(project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify find work area list is denied for non existing project`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findOneWithDetailsByProjectIdentifier(ProjectId()) }

  /* Only one WorkAreaList can exist per Project.
   * This is why we need a createWorkAreaList() to use on demand.
   * Otherwise, we could not test the creation of the WorkAreaList. */
  private fun createWorkAreaList() =
      eventStreamGenerator.setUserContext("userCsm").submitWorkAreaList {
        it.workAreas = listOf(getByReference("workAreaOne"), getByReference("workAreaTwo"))
      }
}
