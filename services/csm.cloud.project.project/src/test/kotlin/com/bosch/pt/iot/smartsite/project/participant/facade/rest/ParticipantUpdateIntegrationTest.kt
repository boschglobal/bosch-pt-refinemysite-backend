/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.UpdateParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK

@DisplayName("Verify updating a participant")
@EnableAllKafkaListeners
class ParticipantUpdateIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var participantController: ParticipantController

  private val userActiveFm by lazy { repositories.findUser(getIdentifier("userActiveFm"))!! }
  private val userValidation by lazy { repositories.findUser(getIdentifier("userValidation"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val participantActiveCsm by lazy {
    repositories.findParticipant(getIdentifier("participantActiveCsm"))!!
  }
  private val participantActiveFm by lazy {
    repositories.findParticipant(getIdentifier("participantActiveFm"))!!
  }
  private val participantInactive by lazy {
    repositories.findParticipant(getIdentifier("participantInactive"))!!
  }
  private val participantValidation by lazy {
    repositories.findParticipant(getIdentifier("participantValidation"))!!
  }
  private val participantInvited by lazy {
    repositories.findParticipant(getIdentifier("participantInvited"))!!
  }
  private val updateParticipantResource by lazy { UpdateParticipantResource(CR) }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany()
        .submitProject()
        .submitUserEmployeeAndParticipant("ActiveCsm", ParticipantRoleEnumAvro.CSM)
        .setUserContext("userActiveCsm")
        .submitUserEmployeeAndParticipant("ActiveFm", ParticipantRoleEnumAvro.FM)
        .submitUserEmployeeAndParticipant(
            "Inactive", ParticipantRoleEnumAvro.FM, ParticipantStatusEnumAvro.INACTIVE)
        .submitUserEmployeeAndParticipant(
            "Validation", ParticipantRoleEnumAvro.FM, ParticipantStatusEnumAvro.VALIDATION)
        .submitParticipantG3("participantInvited") {
          it.user = null
          it.company = null
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INVITED
        }
        .submitInvitation()

    setAuthentication("userActiveCsm")
  }

  @Test
  @DisplayName("in status invited is successful")
  fun updateInvitedParticipantSucceeds() {
    val response =
        participantController.updateParticipant(
            participantInvited.identifier, updateParticipantResource, participantInvited.toEtag())

    assertThat(response.statusCode).isEqualTo(OK)

    response.body.also {
      it?.also {
        assertThat(it.user).isNull()
        assertThat(it.project.identifier.asProjectId()).isEqualTo(project.identifier)
        assertThat(it.projectRole).isEqualTo(CR)
      }
    }
  }

  @Test
  @DisplayName("in status validation is successful")
  fun updateInvalidationParticipantSucceeds() {
    val response =
        participantController.updateParticipant(
            participantValidation.identifier,
            updateParticipantResource,
            participantValidation.toEtag())
    assertThat(response.statusCode).isEqualTo(OK)

    response.body.also {
      it?.also {
        assertThat(it.user!!.identifier).isEqualTo(userValidation.identifier)
        assertThat(it.project.identifier.asProjectId()).isEqualTo(project.identifier)
        assertThat(it.projectRole).isEqualTo(CR)
      }
    }
  }

  @Test
  @DisplayName("in status active is successful")
  fun updateActiveParticipantSucceeds() {
    val response =
        participantController.updateParticipant(
            participantActiveFm.identifier, updateParticipantResource, participantActiveFm.toEtag())
    assertThat(response.statusCode).isEqualTo(OK)

    response.body.also {
      it?.also {
        assertThat(it.user!!.identifier).isEqualTo(userActiveFm.identifier)
        assertThat(it.project.identifier.asProjectId()).isEqualTo(project.identifier)
        assertThat(it.projectRole).isEqualTo(CR)
      }
    }
  }

  @Test
  @DisplayName("in status inactive fails")
  fun updateInactiveParticipantFails() {
    val updateParticipantResource = UpdateParticipantResource(CR)
    assertThatThrownBy {
          participantController.updateParticipant(
              participantInactive.identifier,
              updateParticipantResource,
              participantInactive.toEtag())
        }
        .isInstanceOf(PreconditionViolationException::class.java)
  }

  @Test
  @DisplayName("fails if role of last CSM is changed")
  fun updateParticipantFailsForLastCsm() {
    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      participantController.updateParticipant(
          participantActiveCsm.identifier, updateParticipantResource, participantActiveCsm.toEtag())
    }
  }
}

private fun EventStreamGenerator.submitUserEmployeeAndParticipant(
    referenceSuffix: String,
    participantRole: ParticipantRoleEnumAvro,
    participantStatus: ParticipantStatusEnumAvro = ParticipantStatusEnumAvro.ACTIVE,
): EventStreamGenerator {
  return submitUser("user$referenceSuffix")
      .submitEmployee("employee$referenceSuffix")
      .submitParticipantG3("participant$referenceSuffix") {
        it.role = participantRole
        it.status = participantStatus
      }
}
