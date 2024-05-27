/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.listener.stategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCrParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCsmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [RestDocumentationExtension::class, SpringExtension::class])
@DisplayName("State must")
@SmartSiteSpringBootTest
class UpdateStateFromParticipantEventTest : BaseNotificationTest() {

  @BeforeEach
  override fun setup() {
    super.setup()
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `not be updated for participant pending event`() {
    val count = repositories.participantRepository.count()
    eventStreamGenerator.submitParticipantG3(asReference = "invited-participant") {
      it.role = ParticipantRoleEnumAvro.CR
      it.status = INVITED
    }
    assertThat(repositories.participantRepository.count()).isEqualTo(count)
  }

  @Test
  fun `not be updated for participant in-validation event`() {
    val count = repositories.participantRepository.count()
    eventStreamGenerator.submitParticipantG3(asReference = "validation-participant") {
      it.role = ParticipantRoleEnumAvro.CR
      it.status = VALIDATION
    }
    assertThat(repositories.participantRepository.count()).isEqualTo(count)
  }

  @Test
  fun `not be updated for participant cancelled event`() {
    eventStreamGenerator.submitCrParticipant()
    val count = repositories.participantRepository.count()
    eventStreamGenerator.submitParticipantG3(asReference = CR_PARTICIPANT, eventType = CANCELLED) {
      it.status = INVITED
    }
    assertThat(repositories.participantRepository.count()).isEqualTo(count)
  }
}
