/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.REACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromParticipantEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user")
  }

  @Test
  fun `is updated after participant created event`() {
    val count = repositories.participantRepository.findAll().size

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitCompany(asReference = "new company").submitParticipantG3(
              asReference = "fm-participant-new") { it.role = FM }
    }

    assertThat(repositories.participantRepository.findAll()).hasSize(count + 1)
  }

  @Test
  fun `is updated after participant updated event`() {
    val count = repositories.participantRepository.findAll().size

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitParticipantG3(
          asReference = "fm-participant", eventType = UPDATED) { it.role = CSM }
    }

    assertThat(repositories.participantRepository.findAll()).hasSize(count)
  }

  @Test
  fun `is updated after participant deactivated and reactivated event`() {
    val count = repositories.participantRepository.findAll().size

    // Deactivate participant
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitParticipantG3(
          asReference = "fm-participant", eventType = DEACTIVATED)
    }
    assertThat(repositories.participantRepository.findAll()).hasSize(count)

    // Reactivate participant
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitParticipantG3(
          asReference = "fm-participant", eventType = REACTIVATED)
    }
    assertThat(repositories.participantRepository.findAll()).hasSize(count)
  }

  @Test
  fun `is not updated after participant pending event`() {
    val count = repositories.participantRepository.findAll().size

    eventStreamGenerator.submitParticipantG3(asReference = "fm-participant-pending") {
      it.status = INVITED
    }

    assertThat(repositories.participantRepository.findAll()).hasSize(count)
  }

  @Test
  fun `is not updated after participant in-validation event`() {
    val count = repositories.participantRepository.findAll().size

    eventStreamGenerator.submitParticipantG3(asReference = "fm-participant-in-validation") {
      it.status = VALIDATION
    }

    assertThat(repositories.participantRepository.findAll()).hasSize(count)
  }

  @Test
  fun `is not updated after participant cancelled event`() {
    val count = repositories.participantRepository.findAll().size

    eventStreamGenerator
        .submitParticipantG3(asReference = "fm-participant-pending") { it.status = INVITED }
        .submitParticipantG3(asReference = "fm-participant-pending", eventType = CANCELLED)

    assertThat(repositories.participantRepository.findAll()).hasSize(count)
  }
}
