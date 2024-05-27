/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder

@EnableKafkaListeners
@SmartSiteSpringBootTest
class ParticipantTests : AbstractIntegrationTest() {

  private val participantCsm1 by lazy {
    repositories.participantMappingRepository.findOneByParticipantIdentifier(
        getIdentifier("participantCsm1"))!!
  }

  @BeforeEach
  fun beforeEach() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany()
        .submitUserAndActivate(asReference = "userCsm1")
        .submitEmployee { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(
            asReference = "participantCsm1", aggregateModifications = { it.role = FM })
  }

  @AfterEach
  fun cleanupBase() {
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  @Test
  fun `check that participants are created for participants events`() {
    assertThat(participantCsm1.participantIdentifier).isNotNull
  }

  @Test
  fun `check that existing participant is updated for a participant updated event`() {
    eventStreamGenerator
        .submitParticipantG3(
            asReference = "participantCsm1",
            aggregateModifications = {
              it.role = FM
              it.status = INVITED
            })
        .submitParticipantG3(
            asReference = "participantCsm1",
            aggregateModifications = {
              it.role = FM
              it.status = VALIDATION
            })
        .submitParticipantG3(
            asReference = "participantCsm1",
            aggregateModifications = {
              it.role = FM
              it.status = ACTIVE
            })

    val participant =
        repositories.participantMappingRepository.findOneByParticipantIdentifier(
            participantCsm1.participantIdentifier)

    assertThat(participant).isNotNull
    assertThat(participant!!.participantRole).isEqualTo(FM.toString())
  }

  @Test
  fun `check that a participant event is processed correctly for a non-existing user (a re-processing case) `() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitParticipantG3(asReference = "participant")

    assertThat(
            repositories.participantMappingRepository.findOneByParticipantIdentifier(
                getIdentifier("participant")))
        .isNotNull
  }

  @Test
  fun `check that the participant cancelled event is ignored`() {
    assertThat(repositories.participantMappingRepository.count()).isEqualTo(1)

    eventStreamGenerator
        .submitParticipantG3(
            aggregateModifications = {
              it.role = CSM
              it.status = INVITED
            })
        .submitParticipantG3(
            eventType = ParticipantEventEnumAvro.CANCELLED,
            aggregateModifications = {
              it.role = CSM
              it.status = INVITED
            })

    assertThat(repositories.participantMappingRepository.count()).isEqualTo(1)
  }
}
