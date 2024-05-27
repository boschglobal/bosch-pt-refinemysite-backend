/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_OWN_PARTICIPANT_NOT_REMOVABLE
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_PARTICIPANT_CSM_NOT_REMOVABLE
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.util.withMessageKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NO_CONTENT

@DisplayName("Verify deleting a participant")
@EnableAllKafkaListeners
class ParticipantDeleteIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var participantController: ParticipantController

  @Autowired private lateinit var invitationRepository: InvitationRepository

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUserAndActivate("userCsm")
        .submitCompany()
        .submitEmployee("employeeCsm") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitProject()
        .submitParticipantG3("participantCsm") { it.role = ParticipantRoleEnumAvro.CSM }

    setAuthentication("userCsm")
    projectEventStoreUtils.reset()
  }

  @Test
  @DisplayName("fails when the participant to be removed has the role CSM and is the last one")
  fun verifyDeleteParticipantFailsForLastCsm() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          participantController.deleteParticipant(getIdentifier("participantCsm").asParticipantId())
        }
        .withMessageKey(PARTICIPANT_VALIDATION_ERROR_PARTICIPANT_CSM_NOT_REMOVABLE)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @DisplayName("fails when the participant to be removed is the current user's participant")
  fun verifyDeleteOwnParticipantFails() {
    eventStreamGenerator
        .submitUserAndActivate("userCsm2")
        .submitEmployee("employeeCsm2") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitParticipantG3("participantCsm2") { it.role = ParticipantRoleEnumAvro.CSM }

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          participantController.deleteParticipant(
              getIdentifier("participantCsm2").asParticipantId())
        }
        .withMessageKey(PARTICIPANT_VALIDATION_ERROR_OWN_PARTICIPANT_NOT_REMOVABLE)

    projectEventStoreUtils.verifyEmpty()
  }
  @Test
  @DisplayName(
      "is successful when the participant to be removed has the role CSM and is not the last one")
  fun verifyDeleteParticipantForCsmParticipant() {
    eventStreamGenerator
        .submitUser("userCsm2")
        .submitEmployee("employeeCsm2") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitParticipantG3("participantCsm2") { it.role = ParticipantRoleEnumAvro.CSM }

    projectEventStoreUtils.reset()

    val response =
        participantController.deleteParticipant(getIdentifier("participantCsm2").asParticipantId())
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, DEACTIVATED)
        .getAggregate()
  }

  @Test
  @DisplayName("is successful when the participant to be removed has other role than CSM")
  fun verifyDeleteParticipant() {
    eventStreamGenerator
        .submitUser("userFm")
        .submitEmployee("employeeFm") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitParticipantG3("participantFm") { it.role = ParticipantRoleEnumAvro.FM }

    projectEventStoreUtils.reset()

    val response =
        participantController.deleteParticipant(getIdentifier("participantFm").asParticipantId())
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, DEACTIVATED)
        .getAggregate()
  }

  @Test
  @DisplayName("is successful when the participant in validation is deleted")
  fun verifyDeleteInvalidationParticipant() {
    eventStreamGenerator
        .submitUser("userInValidation")
        .submitParticipantG3("participantInValidation") {
          it.status = ParticipantStatusEnumAvro.VALIDATION
        }
        .submitInvitation()

    invitationEventStoreUtils.reset()
    projectEventStoreUtils.reset()

    val response =
        participantController.deleteParticipant(
            getIdentifier("participantInValidation").asParticipantId())
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)
    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, CANCELLED)
        .getAggregate()
  }

  @Test
  @DisplayName("is successful when the invited participant is deleted")
  fun verifyDeleteInvitedParticipant() {
    eventStreamGenerator
        .submitUser("userInvited")
        .submitParticipantG3("participantInvited") {
          it.status = ParticipantStatusEnumAvro.INVITED
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitInvitation()

    val invitedParticipantIdentifier = getIdentifier("participantInvited")
    assertThat(
            invitationRepository.findOneByParticipantIdentifier(
                invitedParticipantIdentifier.asParticipantId()))
        .isNotNull

    invitationEventStoreUtils.reset()
    projectEventStoreUtils.reset()

    val response =
        participantController.deleteParticipant(invitedParticipantIdentifier.asParticipantId())
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)
    projectEventStoreUtils.verifyContainsAndGet(ParticipantEventG3Avro::class.java, CANCELLED)

    assertThat(
            invitationRepository.findOneByParticipantIdentifier(
                invitedParticipantIdentifier.asParticipantId()))
        .isNull()
    invitationEventStoreUtils.verifyContainsTombstoneMessageAndGet(1)
  }

  @Test
  @DisplayName(
      "is successful when the invited participant (being a CSM) is deleted and only one other CSM exists")
  fun verifyDeleteInvitedCsmParticipant() {
    eventStreamGenerator
        .submitUser("userInvited")
        .submitParticipantG3("participantInvited") {
          it.status = ParticipantStatusEnumAvro.INVITED
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitInvitation()

    val invitedParticipantIdentifier = getIdentifier("participantInvited")
    assertThat(
            invitationRepository.findOneByParticipantIdentifier(
                invitedParticipantIdentifier.asParticipantId()))
        .isNotNull

    invitationEventStoreUtils.reset()
    projectEventStoreUtils.reset()

    val response =
        participantController.deleteParticipant(invitedParticipantIdentifier.asParticipantId())
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)
    projectEventStoreUtils.verifyContainsAndGet(ParticipantEventG3Avro::class.java, CANCELLED)

    assertThat(
            invitationRepository.findOneByParticipantIdentifier(
                invitedParticipantIdentifier.asParticipantId()))
        .isNull()
    invitationEventStoreUtils.verifyContainsTombstoneMessageAndGet(1)
  }
}
