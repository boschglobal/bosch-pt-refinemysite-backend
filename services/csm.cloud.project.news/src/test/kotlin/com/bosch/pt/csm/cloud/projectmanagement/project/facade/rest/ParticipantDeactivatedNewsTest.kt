/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ParticipantDeactivatedNewsTest : AbstractNewsTest() {

  @BeforeEach
  fun initialize() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitParticipantG3(
            asReference = "fm-participant", eventType = ParticipantEventEnumAvro.DEACTIVATED)
  }

  @Test
  fun `all news for that participant should be gone`() {
    assertThat(controller.getDetails(employeeFm, taskAggregateIdentifier)).isEmpty()
  }

  @Test
  fun `no news are recorded anymore for that participant`() {
    eventStreamGenerator.submitTaskAttachment()
    assertThat(controller.getDetails(employeeFm, taskAggregateIdentifier)).isEmpty()
  }

  @Test
  fun `news are recorded again once he was reactivated`() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitParticipantG3(
            asReference = "fm-participant", eventType = ParticipantEventEnumAvro.REACTIVATED)
        .setUserContext("csm-user-1")
        .submitTaskAttachment()
    assertThat(controller.getDetails(employeeFm, taskAggregateIdentifier)).isNotEmpty
  }
}
