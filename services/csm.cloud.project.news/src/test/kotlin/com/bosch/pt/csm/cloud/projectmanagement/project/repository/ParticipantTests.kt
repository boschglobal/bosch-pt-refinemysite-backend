/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.repository

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SmartSiteSpringBootTest
open class ParticipantTests : AbstractEventStreamIntegrationTest() {

  @Autowired private lateinit var participantMappingRepository: ParticipantMappingRepository

  @BeforeEach
  fun beforeEach() {
    eventStreamGenerator
        .submitUserAndActivate(asReference = "csm-user") {
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
        }
        .submitCompany(asReference = "csm-company")
        .submitEmployee(asReference = "csm-employee") {
          it.user = getByReference("csm-user")
          it.company = getByReference("csm-company")
        }
        .submitProject()
  }

  @Test
  fun `check that participants are created for participants events`() {
    submitCsmParticipant()

    val participants = participantMappingRepository.findAll()
    assertThat(participants).hasSize(1)
    assertThat(participants.first().participantRole).isEqualTo(CSM.toString())
  }

  @Test
  fun `check that existing participants is updated for a participants updated event`() {
    submitCsmParticipant()

    eventStreamGenerator.submitParticipantG3 {
      it.company = getByReference("csm-company")
      it.user = getByReference("csm-user")
      it.role = FM
    }
    val participants = participantMappingRepository.findAll()
    assertThat(participants).hasSize(1)
    assertThat(participants.first().participantRole).isEqualTo(FM.toString())
  }

  @Test
  fun `check that the participant cancelled event is ignored`() {
    var participants = participantMappingRepository.findAll()
    assertThat(participants).hasSize(0)

    eventStreamGenerator
        .submitParticipantG3 {
          it.company = getByReference("csm-company")
          it.role = CSM
          it.status = INVITED
        }
        .submitParticipantG3(eventType = CANCELLED) {
          it.company = getByReference("csm-company")
          it.role = CSM
          it.status = INVITED
        }

    participants = participantMappingRepository.findAll()
    assertThat(participants).hasSize(0)
  }

  private fun submitCsmParticipant() {
    eventStreamGenerator
        .submitParticipantG3 {
          it.company = getByReference("csm-company")
          it.role = CSM
          it.status = INVITED
        }
        .submitParticipantG3 {
          it.company = getByReference("csm-company")
          it.role = CSM
          it.status = VALIDATION
        }
        .submitParticipantG3 {
          it.company = getByReference("csm-company")
          it.role = CSM
          it.status = ACTIVE
        }
  }
}
