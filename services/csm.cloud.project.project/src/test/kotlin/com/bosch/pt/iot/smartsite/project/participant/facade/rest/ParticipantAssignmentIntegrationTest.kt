/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.config.MailjetPort
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.AssignParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantAddedTemplate
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantInvitedTemplate
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.util.MailTestHelper
import com.bosch.pt.iot.smartsite.util.respondWithSuccess
import com.bosch.pt.iot.smartsite.util.templateId
import java.time.ZoneOffset.UTC
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED

@DisplayName("Verify assigning ... ")
@EnableAllKafkaListeners
class ParticipantAssignmentIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var mailjetPort: MailjetPort

  @Autowired private lateinit var cut: ParticipantController

  @Autowired private lateinit var employeeRepository: EmployeeRepository

  @Autowired private lateinit var invitationRepository: InvitationRepository

  @Autowired private lateinit var mailTestHelper: MailTestHelper

  private val mockMailjetServer by lazy { MockWebServer().apply { start(mailjetPort.value) } }

  private val company by lazy { repositories.findCompany(getIdentifier("company"))!! }
  private val userAssignedToCompany by lazy {
    repositories.findUser(getIdentifier("userAssignedToCompany"))!!
  }
  private val employeeAssignedToCompany by lazy {
    repositories.findEmployee(getIdentifier("employeeAssignedToCompany"))!!
  }
  private val inactiveUser by lazy { repositories.findUser(getIdentifier("inactiveUser"))!! }
  private val inactiveEmployee by lazy {
    repositories.findEmployee(getIdentifier("inactiveEmployee"))!!
  }
  private val newUserNotAssignedToCompany by lazy {
    repositories.findUser(getIdentifier("newUserNotAssignedToCompany"))!!
  }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val emailUnregisteredUser = randomUUID().toString() + "@example.com"

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("userAssignedToCompany")
        .submitEmployee("employeeAssignedToCompany")
        .submitUser("inactiveUser")
        .submitEmployee("inactiveEmployee")
        .submitParticipantG3("inactiveParticipant") {
          it.status = ParticipantStatusEnumAvro.INACTIVE
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitUser("newUserNotAssignedToCompany")

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
  }

  @AfterEach fun tearDown() = mockMailjetServer.close()

  @Nested
  @DisplayName("a user that is registered and assigned to a company")
  inner class VerifyAssigningRegisteredUsersWithEmployeeIsSuccessful {

    @BeforeEach fun setup() = mockMailjetServer.respondWithSuccess()

    @AfterEach
    fun tearDown() {
      invitationEventStoreUtils.verifyEmpty()
      assertMailSent(ParticipantAddedTemplate.TEMPLATE_NAME)
    }

    @Test
    @DisplayName("as a CSM is successful")
    fun verifyAssignParticipantAsCsmIsSuccessful() {
      val assignParticipantResource = AssignParticipantResource(userAssignedToCompany.email!!, CSM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.user!!.identifier)
          .isEqualTo(userAssignedToCompany.identifier)
      assertThat(participantResource.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.projectRole).isEqualTo(CSM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser())
                .isEqualByComparingTo(userAssignedToCompany.toAggregateIdentifier())
            assertThat(it.getCompany()).isEqualByComparingTo(company.toAggregateIdentifier())
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CSM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }

    @Test
    @DisplayName("as a CR is successful")
    fun verifyAssignParticipantAsCrIsSuccessful() {
      val assignParticipantResource = AssignParticipantResource(userAssignedToCompany.email!!, CR)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.user!!.identifier)
          .isEqualTo(userAssignedToCompany.identifier)
      assertThat(participantResource.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.projectRole).isEqualTo(CR)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser())
                .isEqualByComparingTo(userAssignedToCompany.toAggregateIdentifier())
            assertThat(it.getCompany()).isEqualByComparingTo(company.toAggregateIdentifier())
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CR)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }

    @Test
    @DisplayName("as an FM of the same company is successful")
    fun assignParticipantAsFmIsSuccessful() {
      val assignParticipantResource =
          AssignParticipantResource(employeeAssignedToCompany.user!!.email!!, FM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.user!!.identifier)
          .isEqualTo(userAssignedToCompany.identifier)
      assertThat(participantResource.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.projectRole).isEqualTo(FM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser())
                .isEqualByComparingTo(userAssignedToCompany.toAggregateIdentifier())
            assertThat(it.getCompany()).isEqualByComparingTo(company.toAggregateIdentifier())
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.FM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }
  }

  @Nested
  @DisplayName("a user that is registered but not yet assigned to a company")
  inner class VerifyAssigningRegisteredUsersWithoutEmployeeIsSuccessful {

    @AfterEach
    fun tearDown() {
      invitationEventStoreUtils.verifyEmpty()
      assertNoMailSent()
    }

    @Test
    @DisplayName("as a CSM is successful")
    fun verifyAssignParticipantAsCsmIsSuccessful() {
      val assignParticipantResource =
          AssignParticipantResource(newUserNotAssignedToCompany.email!!, CSM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier)
          .isEqualTo(newUserNotAssignedToCompany.identifier)
      assertThat(participantResource.email).isEqualTo(newUserNotAssignedToCompany.email)
      assertThat(participantResource.company).isNull()
      assertThat(participantResource.projectRole).isEqualTo(CSM)
      assertThat(participantResource.status).isEqualTo(VALIDATION)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser())
                .isEqualByComparingTo(newUserNotAssignedToCompany.toAggregateIdentifier())
            assertThat(it.getCompany()).isNull()
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CSM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.VALIDATION)
          }
    }

    @Test
    @DisplayName("as a CR is successful")
    fun verifyAssignParticipantAsCrIsSuccessful() {
      val assignParticipantResource =
          AssignParticipantResource(newUserNotAssignedToCompany.email!!, CR)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier)
          .isEqualTo(newUserNotAssignedToCompany.identifier)
      assertThat(participantResource.email).isEqualTo(newUserNotAssignedToCompany.email)
      assertThat(participantResource.company).isNull()
      assertThat(participantResource.projectRole).isEqualTo(CR)
      assertThat(participantResource.status).isEqualTo(VALIDATION)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser())
                .isEqualByComparingTo(newUserNotAssignedToCompany.toAggregateIdentifier())
            assertThat(it.getCompany()).isNull()
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CR)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.VALIDATION)
          }
    }

    @Test
    @DisplayName("as an FM is successful")
    fun verifyAssignParticipantAsFmIsSuccessful() {
      val assignParticipantResource =
          AssignParticipantResource(newUserNotAssignedToCompany.email!!, FM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier)
          .isEqualTo(newUserNotAssignedToCompany.identifier)
      assertThat(participantResource.email).isEqualTo(newUserNotAssignedToCompany.email)
      assertThat(participantResource.company).isNull()
      assertThat(participantResource.projectRole).isEqualTo(FM)
      assertThat(participantResource.status).isEqualTo(VALIDATION)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser())
                .isEqualByComparingTo(newUserNotAssignedToCompany.toAggregateIdentifier())
            assertThat(it.getCompany()).isNull()
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.FM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.VALIDATION)
          }
    }

    @Test
    @DisplayName("where no invitation was created fails if the invitation should be resent")
    fun verifyInvitationResendFailsIf() {
      eventStreamGenerator.submitParticipantG3("newParticipant") {
        it.status = ParticipantStatusEnumAvro.INVITED
        it.user = newUserNotAssignedToCompany.toAggregateIdentifier()
      }

      assertThatExceptionOfType(AggregateNotFoundException::class.java).isThrownBy {
        cut.resendInvitation(getIdentifier("newParticipant").asParticipantId())
      }
    }
  }

  @Nested
  @DisplayName("a user that is not yet registered")
  inner class VerifyAssigningUsersNotYetRegisteredIsSuccessful {

    @BeforeEach
    fun setup() {
      mockMailjetServer.respondWithSuccess()
    }

    @AfterEach
    fun tearDown() {
      assertMailSent(ParticipantInvitedTemplate.TEMPLATE_NAME)
    }

    @Test
    @DisplayName("as a CSM is successful")
    fun verifyAssignParticipantAsCsmIsSuccessful() {
      val assignParticipantResource = AssignParticipantResource(emailUnregisteredUser, CSM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user).isNull()
      assertThat(participantResource.email).isEqualTo(emailUnregisteredUser)
      assertThat(participantResource.company).isNull()
      assertThat(participantResource.projectRole).isEqualTo(CSM)
      assertThat(participantResource.status).isEqualTo(ParticipantStatusEnum.INVITED)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser()).isNull()
            assertThat(it.getCompany()).isNull()
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CSM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.INVITED)
          }

      val invitation =
          invitationRepository.findOneByProjectIdentifierAndEmail(
              projectIdentifier, emailUnregisteredUser)!!

      invitationEventStoreUtils
          .verifyContainsAndGet(InvitationEventAvro::class.java, InvitationEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getProjectIdentifier()).isEqualTo(projectIdentifier.toString())
            assertThat(it.getParticipantIdentifier())
                .isEqualTo(invitation.participantIdentifier.toString())
            assertThat(it.getEmail()).isEqualTo(emailUnregisteredUser)
            assertThat(it.getLastSent())
                .isEqualTo(invitation.lastSent.toInstant(UTC).toEpochMilli())
          }
    }

    @Test
    @DisplayName("as a CR is successful")
    fun verifyAssignParticipantAsCrIsSuccessful() {
      val assignParticipantResource = AssignParticipantResource(emailUnregisteredUser, CR)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user).isNull()
      assertThat(participantResource.email).isEqualTo(emailUnregisteredUser)
      assertThat(participantResource.company).isNull()
      assertThat(participantResource.projectRole).isEqualTo(CR)
      assertThat(participantResource.status).isEqualTo(ParticipantStatusEnum.INVITED)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser()).isNull()
            assertThat(it.getCompany()).isNull()
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CR)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.INVITED)
          }

      val invitation =
          invitationRepository.findOneByProjectIdentifierAndEmail(
              projectIdentifier, emailUnregisteredUser)!!

      invitationEventStoreUtils
          .verifyContainsAndGet(InvitationEventAvro::class.java, InvitationEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getProjectIdentifier()).isEqualTo(projectIdentifier.toString())
            assertThat(it.getParticipantIdentifier())
                .isEqualTo(invitation.participantIdentifier.toString())
            assertThat(it.getEmail()).isEqualTo(emailUnregisteredUser)
            assertThat(it.getLastSent())
                .isEqualTo(invitation.lastSent.toInstant(UTC).toEpochMilli())
          }
    }

    @Test
    @DisplayName("as an FM is successful")
    fun assignParticipantAsFmIsSuccessful() {
      val assignParticipantResource = AssignParticipantResource(emailUnregisteredUser, FM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user).isNull()
      assertThat(participantResource.email).isEqualTo(emailUnregisteredUser)
      assertThat(participantResource.company).isNull()
      assertThat(participantResource.projectRole).isEqualTo(FM)
      assertThat(participantResource.status).isEqualTo(ParticipantStatusEnum.INVITED)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser()).isNull()
            assertThat(it.getCompany()).isNull()
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.FM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.INVITED)
          }

      val invitation =
          invitationRepository.findOneByProjectIdentifierAndEmail(
              projectIdentifier, emailUnregisteredUser)!!

      invitationEventStoreUtils
          .verifyContainsAndGet(InvitationEventAvro::class.java, InvitationEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getProjectIdentifier()).isEqualTo(projectIdentifier.toString())
            assertThat(it.getParticipantIdentifier())
                .isEqualTo(invitation.participantIdentifier.toString())
            assertThat(it.getEmail()).isEqualTo(emailUnregisteredUser)
            assertThat(it.getLastSent())
                .isEqualTo(invitation.lastSent.toInstant(UTC).toEpochMilli())
          }
    }
  }

  @Nested
  @DisplayName("a user that is currently an inactive participant")
  inner class VerifyAssigningUsersThatAreInactiveParticipantsIsSuccessful {

    @BeforeEach
    fun setup() {
      mockMailjetServer.respondWithSuccess()
    }

    @AfterEach
    fun tearDown() {
      invitationEventStoreUtils.verifyEmpty()
      assertMailSent(ParticipantAddedTemplate.TEMPLATE_NAME)
    }

    @Test
    @DisplayName("with a new role is successful")
    fun assignInactiveParticipantAgainWithNewRole() {
      val assignParticipantResource = AssignParticipantResource(inactiveUser.email!!, FM)

      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier).isEqualTo(inactiveUser.identifier)
      assertThat(participantResource.company!!.identifier).isEqualTo(company.identifier)
      assertThat(participantResource.projectRole).isEqualTo(FM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.REACTIVATED)
          .getAggregate()
          .also {
            assertThat(it.getUser()).isEqualByComparingTo(inactiveUser.toAggregateIdentifier())
            assertThat(it.getCompany()).isEqualByComparingTo(company.toAggregateIdentifier())
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.FM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }

    @Test
    @DisplayName("with a new company is successful")
    fun assignInactiveParticipantAgainWithNewCompany() {
      eventStreamGenerator.submitCompany("newCompany").submitEmployee("newEmployee") {
        it.user = inactiveUser.toAggregateIdentifier()
      }

      val newCompany = repositories.findCompany(getIdentifier("newCompany"))!!
      employeeRepository.delete(inactiveEmployee)
      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantResource(inactiveUser.email!!, FM)
      val response = cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier).isEqualTo(inactiveUser.identifier)
      assertThat(participantResource.company!!.identifier).isEqualTo(newCompany.identifier)
      assertThat(participantResource.projectRole).isEqualTo(FM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .getAggregate()
          .also {
            assertThat(it.getUser()).isEqualByComparingTo(inactiveUser.toAggregateIdentifier())
            assertThat(it.getCompany()).isEqualByComparingTo(newCompany.toAggregateIdentifier())
            assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.FM)
            assertThat(it.getStatus()).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }
  }

  @Nested
  @DisplayName("fails in case")
  inner class VerifyAssigningUsersFails {

    @Test
    @DisplayName("the user is already an active participant")
    fun assigningAUserThatIsAlreadyAParticipantFails() {
      eventStreamGenerator.submitParticipantG3("newParticipant") {
        it.user = userAssignedToCompany.toAggregateIdentifier()
        it.company = company.toAggregateIdentifier()
        it.role = ParticipantRoleEnumAvro.FM
      }

      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantResource(userAssignedToCompany.email!!, FM)

      assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
        cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      }
    }

    @Test
    @DisplayName("the user is already a participant in validation")
    fun assigningAUserThatIsAlreadyAnInvalidationParticipantFails() {
      eventStreamGenerator.submitParticipantG3("newParticipant") {
        it.user = newUserNotAssignedToCompany.toAggregateIdentifier()
        it.company = null
        it.role = ParticipantRoleEnumAvro.FM
        it.status = ParticipantStatusEnumAvro.VALIDATION
      }

      projectEventStoreUtils.reset()

      val assignParticipantResource =
          AssignParticipantResource(newUserNotAssignedToCompany.email!!, FM)

      assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
        cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      }
    }

    @Test
    @DisplayName("the user is already an invited participant")
    fun assigningAUserThatIsAlreadyAPendingParticipantFails() {
      eventStreamGenerator
          .submitParticipantG3("newParticipant") {
            it.user = null
            it.company = null
            it.role = ParticipantRoleEnumAvro.FM
            it.status = ParticipantStatusEnumAvro.INVITED
          }
          .submitInvitation("newInvite") { it.email = emailUnregisteredUser }

      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantResource(emailUnregisteredUser, FM)

      assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
        cut.assignParticipant(projectIdentifier, null, assignParticipantResource)
      }
    }
  }

  private fun assertMailSent(templateName: String) {
    val expectAnyOfTemplateIds: Set<Long?> = mailTestHelper.findAllTemplateIds(templateName)
    assertThat(mockMailjetServer.takeRequest(5, TimeUnit.SECONDS)?.templateId())
        .isIn(expectAnyOfTemplateIds)
    assertThat(mockMailjetServer.requestCount).isEqualTo(1)
  }

  private fun assertNoMailSent() {
    assertThat(mockMailjetServer.requestCount).isEqualTo(0)
  }
}
