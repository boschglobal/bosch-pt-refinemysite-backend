/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.AssignParticipantAsAdminResource
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED

@DisplayName("Verify assigning ... ")
@EnableAllKafkaListeners
class ParticipantAssignmentAsAdminIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ParticipantController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  private val userCsm1 by lazy { get<UserAggregateAvro>("csm-user")!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUser("admin") { it.admin = true }
        .plainProjectWithCsm()

    projectEventStoreUtils.reset()
    setAuthentication("admin")
  }

  @Nested
  @DisplayName("a user that is not assigned to a company")
  inner class VerifyAssigningUsersWithoutEmployeeFails {

    @Test
    @DisplayName("cannot be reassigned to a project by the admin")
    fun verifyAssignParticipantWithoutEmployeeAsAdminFails() {
      val email = "test2@example.com"
      eventStreamGenerator.submitUser(asReference = "user2") { it.email = email }
      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantAsAdminResource(email)

      assertThatExceptionOfType(IllegalArgumentException::class.java)
          .isThrownBy { cut.assignParticipantAsAdmin(projectIdentifier, assignParticipantResource) }
          .withMessage("Employee of user to assign not found")

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  @DisplayName("valid project")
  inner class VerifyAssigningUsersWithoutProjectFails {

    @Test
    @DisplayName("cannot be reassigned by the admin")
    fun verifyAssignParticipantWithInvalidProjectAsAdminFails() {
      val email = "test2@example.com"
      eventStreamGenerator
          .submitUser(asReference = "user2") { it.email = email }
          .submitEmployee("employee-user2") { it.user = getByReference("user2") }
      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantAsAdminResource(email)

      assertThatExceptionOfType(IllegalArgumentException::class.java)
          .isThrownBy {
            cut.assignParticipantAsAdmin(randomUUID().asProjectId(), assignParticipantResource)
          }
          .withMessage("Project not found")

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  @DisplayName("a user that already has a participant")
  inner class VerifyAssigningUsersWithExistingParticipant {

    @Test
    @DisplayName("cannot be reassigned if the existing participant is active")
    fun reassignActiveParticipantFails() {
      val assignParticipantResource = AssignParticipantAsAdminResource(userCsm1.email!!)

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy { cut.assignParticipantAsAdmin(projectIdentifier, assignParticipantResource) }
          .withMessageKey(PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    @DisplayName("can be reactivated")
    fun reactivateParticipantSucceeds() {
      eventStreamGenerator.submitParticipantG3("csm-participant", eventType = DEACTIVATED) {
        it.status = INACTIVE
      }
      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantAsAdminResource(userCsm1.email!!)
      val response = cut.assignParticipantAsAdmin(projectIdentifier, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier).isEqualTo(userCsm1.getIdentifier())
      assertThat(participantResource.company!!.identifier).isEqualTo(getIdentifier("company"))
      assertThat(participantResource.projectRole).isEqualTo(CSM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.REACTIVATED)
          .aggregate
          .also {
            assertThat(it.user).isEqualByComparingTo(userCsm1.aggregateIdentifier)
            assertThat(it.company).isEqualByComparingTo(getByReference("company"))
            assertThat(it.role).isEqualTo(ParticipantRoleEnumAvro.CSM)
            assertThat(it.status).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }
  }

  @Nested
  @DisplayName("a user that has changed company")
  inner class VerifyAssigningUsersWithChangedCompany {

    @Test
    @DisplayName("can be assigned to the project as new participant")
    fun assignAsNewParticipant() {
      eventStreamGenerator
          .submitParticipantG3("csm-participant", eventType = DEACTIVATED) { it.status = INACTIVE }
          .submitEmployee("csm-employee", eventType = EmployeeEventEnumAvro.DELETED)
          .submitCompany("newCompany")
          .submitEmployee("newEmployee") { it.user = userCsm1.aggregateIdentifier }

      projectEventStoreUtils.reset()

      val assignParticipantResource = AssignParticipantAsAdminResource(userCsm1.email!!)
      val response = cut.assignParticipantAsAdmin(projectIdentifier, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier).isEqualTo(userCsm1.getIdentifier())
      assertThat(participantResource.company!!.identifier).isEqualTo(getIdentifier("newCompany"))
      assertThat(participantResource.projectRole).isEqualTo(CSM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .aggregate
          .also {
            assertThat(it.user).isEqualByComparingTo(userCsm1.aggregateIdentifier)
            assertThat(it.company).isEqualByComparingTo(getByReference("newCompany"))
            assertThat(it.role).isEqualTo(ParticipantRoleEnumAvro.CSM)
            assertThat(it.status).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }
  }

  @Nested
  @DisplayName("a user that was no relation to the project")
  inner class VerifyAssigningUsersWithoutExistingParticipant {

    @Test
    @DisplayName("can be assigned to the project as new participant")
    fun assignAsNewParticipant() {

      eventStreamGenerator
          .submitParticipantG3("csm-participant", eventType = DEACTIVATED) { it.status = INACTIVE }
          .submitUser(asReference = "user2")
          .submitEmployee("user2-employee") { it.user = getByReference("user2") }

      projectEventStoreUtils.reset()

      val user2 = get<UserAggregateAvro>("user2")!!

      val assignParticipantResource = AssignParticipantAsAdminResource(user2.email!!)
      val response = cut.assignParticipantAsAdmin(projectIdentifier, assignParticipantResource)
      assertThat(response.statusCode).isEqualTo(CREATED)

      val participantResource = response.body
      assertThat(participantResource).isNotNull
      assertThat(participantResource!!.project.identifier).isEqualTo(projectIdentifier.toUuid())
      assertThat(participantResource.user!!.identifier).isEqualTo(user2.getIdentifier())
      assertThat(participantResource.company!!.identifier).isEqualTo(getIdentifier("company"))
      assertThat(participantResource.projectRole).isEqualTo(CSM)
      assertThat(participantResource.status).isEqualTo(ACTIVE)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED)
          .aggregate
          .also {
            assertThat(it.user).isEqualByComparingTo(user2.aggregateIdentifier)
            assertThat(it.company).isEqualByComparingTo(getByReference("company"))
            assertThat(it.role).isEqualTo(ParticipantRoleEnumAvro.CSM)
            assertThat(it.status).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
          }
    }
  }
}
