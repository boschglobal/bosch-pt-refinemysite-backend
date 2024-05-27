/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.online

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getCompanyIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getCreatedByUserIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getUserIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
open class CompanyEventListenerEmployeeEventsTest : AbstractIntegrationTestV2() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate().submitUser("daniel")
    useOnlineListener()
  }

  @Test
  fun `validate employee created event`() {
    eventStreamGenerator.submitCompanyWithBothAddresses().submitEmployee {
      it.user = getByReference("daniel")
      it.roles = listOf(EmployeeRoleEnumAvro.FM)
    }

    val employeeAggregate = get<EmployeeAggregateAvro>("employee")!!

    repositories.employeeRepository
        .findOneWithDetailsByUserIdentifier(getIdentifier("daniel"))!!
        .apply {
          assertThat(roles!!.map { it.name })
              .isEqualTo(employeeAggregate.getRoles().map { it.name })
          assertThat(company!!.identifier).isEqualTo(employeeAggregate.getCompanyIdentifier())
          assertThat(user!!.identifier).isEqualTo(employeeAggregate.getUserIdentifier())
          assertThat(identifier).isEqualTo(employeeAggregate.getIdentifier())
          assertThat(createdBy.get().identifier)
              .isEqualTo(employeeAggregate.getCreatedByUserIdentifier())
          assertThat(lastModifiedBy.get().identifier)
              .isEqualTo(employeeAggregate.getLastModifiedByUserIdentifier())
          assertThat(version).isEqualTo(employeeAggregate.getVersion())
        }
  }

  @Test
  fun `validate employee updated event`() {
    eventStreamGenerator
        .submitCompanyWithBothAddresses()
        .submitEmployee {
          it.user = getByReference("daniel")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(eventType = EmployeeEventEnumAvro.UPDATED) {
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }

    val employeeAggregate = get<EmployeeAggregateAvro>("employee")!!

    repositories.employeeRepository
        .findOneWithDetailsByUserIdentifier(getIdentifier("daniel"))!!
        .apply {
          assertThat(roles!!.map { it.name })
              .isEqualTo(employeeAggregate.getRoles().map { it.name })
          assertThat(roles).hasSize(1)
          assertThat(roles!!.first()).isEqualTo(EmployeeRoleEnum.CSM)
          assertThat(company!!.identifier).isEqualTo(employeeAggregate.getCompanyIdentifier())
          assertThat(user!!.identifier).isEqualTo(employeeAggregate.getUserIdentifier())
          assertThat(identifier).isEqualTo(employeeAggregate.getIdentifier())
          assertThat(createdBy.get().identifier)
              .isEqualTo(employeeAggregate.getCreatedByUserIdentifier())
          assertThat(lastModifiedBy.get().identifier)
              .isEqualTo(employeeAggregate.getLastModifiedByUserIdentifier())
          assertThat(version).isEqualTo(employeeAggregate.getVersion())
        }
  }

  @Test
  fun `validate employee deleted event`() {
    eventStreamGenerator.submitCompanyWithBothAddresses().submitEmployee {
      it.user = getByReference("daniel")
      it.roles = listOf(EmployeeRoleEnumAvro.FM)
    }

    val employeeIdentifier = getIdentifier("employee")

    assertThat(repositories.employeeRepository.findOneByIdentifier(employeeIdentifier)).isNotNull

    eventStreamGenerator.submitEmployee(eventType = EmployeeEventEnumAvro.DELETED)

    assertThat(repositories.employeeRepository.findOneByIdentifier(employeeIdentifier)).isNull()
  }

  @Test
  fun `validate employee deleted event and participant deactivated`() {
    eventStreamGenerator
        .submitCompanyWithBothAddresses()
        .submitEmployee {
          it.user = getByReference("daniel")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitProject()
        .submitParticipantG3 {
          it.user = getByReference("daniel")
          it.role = ParticipantRoleEnumAvro.FM
        }

    repositories.participantRepository
        .findOneWithDetailsByIdentifier(getIdentifier("participant").asParticipantId())
        .apply {
          assertThat(this).isNotNull
          assertThat(this!!.isActive()).isTrue
          assertThat(company!!.identifier).isEqualTo(getIdentifier("company"))
          assertThat(user!!.identifier).isEqualTo(getIdentifier("daniel"))
          assertThat(project!!.identifier).isEqualTo(getIdentifier("project").asProjectId())
        }

    eventStreamGenerator.submitEmployee(eventType = EmployeeEventEnumAvro.DELETED)

    assertThatEmployeeIsDeleted()
    assertThatParticipantIsDeactivated()
  }

  @Test
  fun `validate that deleting an employee with already deactivated participants succeeds`() {
    eventStreamGenerator
        .submitCompanyWithBothAddresses()
        .submitEmployee()
        .submitProject()
        .submitParticipantG3()
        .submitProject("project2")
        .submitParticipantG3("participant2")
        .submitParticipantG3("participant2", eventType = ParticipantEventEnumAvro.DEACTIVATED) {
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
    assertThatParticipantIsDeactivated(getIdentifier("project2").asProjectId())

    eventStreamGenerator.submitEmployee(eventType = EmployeeEventEnumAvro.DELETED)

    assertThatEmployeeIsDeleted()
    assertThatParticipantIsDeactivated(getIdentifier("project").asProjectId())
  }

  private fun assertThatEmployeeIsDeleted() {
    assertThat(repositories.employeeRepository.findOneByIdentifier(getIdentifier("employee")))
        .isNull()
  }

  private fun assertThatParticipantIsDeactivated(
      projectIdentifier: ProjectId = getIdentifier("project").asProjectId()
  ) {
    repositories.participantRepository
        .findOneByUserIdentifierAndProjectIdentifier(getIdentifier("daniel"), projectIdentifier)!!
        .apply {
          assertThat(this).isNotNull
          assertThat(this.status).isEqualTo(ParticipantStatusEnum.INACTIVE)
        }
  }
}
