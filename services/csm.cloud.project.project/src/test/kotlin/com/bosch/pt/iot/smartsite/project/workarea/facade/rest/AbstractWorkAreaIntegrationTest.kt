/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import org.junit.jupiter.api.BeforeEach

abstract class AbstractWorkAreaIntegrationTest : AbstractIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitWorkArea(asReference = "workAreaTwo")
        .submitWorkArea(asReference = "workAreaThree")
        .submitWorkAreaList(eventType = WorkAreaListEventEnumAvro.ITEMADDED) {
          it.workAreas =
              listOf(
                  EventStreamGeneratorStaticExtensions.getByReference("workArea"),
                  EventStreamGeneratorStaticExtensions.getByReference("workAreaTwo"),
                  EventStreamGeneratorStaticExtensions.getByReference("workAreaThree"))
        }
        .submitProject(asReference = "projectTwo")
        .submitParticipantG3(asReference = "participantCsm2") {
          it.user = EventStreamGeneratorStaticExtensions.getByReference("userCsm2")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }
}
