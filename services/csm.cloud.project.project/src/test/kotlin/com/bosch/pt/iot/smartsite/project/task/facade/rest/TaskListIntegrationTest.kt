/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus

@EnableAllKafkaListeners
class TaskListIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var taskController: TaskController

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany("companyA") { it.name = "Company A" }
        .submitCompany("companyB") { it.name = "Company B" }
        .submitUser("userCsm")
        .submitUser("userCr")
        .submitUser("userFm")
        .submitEmployee("employeeCsm") {
          it.company = getByReference("companyA")
          it.user = getByReference("userCsm")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        .submitEmployee("employeeCr") {
          it.company = getByReference("companyB")
          it.user = getByReference("userCr")
          it.roles = listOf(EmployeeRoleEnumAvro.CR)
        }
        .submitEmployee("employeeFm") {
          it.company = getByReference("companyB")
          it.user = getByReference("userFm")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3("csmParticipant") {
          it.role = ParticipantRoleEnumAvro.CSM
          it.company = getByReference("companyA")
          it.user = getByReference("userCsm")
        }
        .submitParticipantG3("crParticipantCompanyB") {
          it.role = ParticipantRoleEnumAvro.CR
          it.company = getByReference("companyB")
          it.user = getByReference("userCr")
        }
        .submitParticipantG3("fmParticipantCompanyB") {
          it.role = ParticipantRoleEnumAvro.FM
          it.company = getByReference("companyB")
          it.user = getByReference("userFm")
        }
        .submitTask("taskA") {
          it.status = TaskStatusEnumAvro.DRAFT
          it.name = "A"
          it.assignee = getByReference("csmParticipant")
        }
        .submitTask("taskB") {
          it.status = TaskStatusEnumAvro.DRAFT
          it.name = "B"
          it.assignee = getByReference("crParticipantCompanyB")
        }
        .submitTask("taskC") {
          it.status = TaskStatusEnumAvro.DRAFT
          it.name = "C"
          it.assignee = getByReference("fmParticipantCompanyB")
        }
  }

  @Test
  fun `verify find all tasks for csm`() {
    setAuthentication(getIdentifier("userCsm"))

    val response =
        taskController.findAll(
            getIdentifier("project").asProjectId(), true, PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.tasks.size).isEqualTo(3)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("A", "B", "C")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all tasks for csm not role based`() {
    setAuthentication(getIdentifier("userCsm"))

    val response =
        taskController.findAll(
            getIdentifier("project").asProjectId(), false, PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.tasks.size).isEqualTo(3)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("A", "B", "C")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all tasks for cr`() {
    setAuthentication(getIdentifier("userCr"))

    val response =
        taskController.findAll(
            getIdentifier("project").asProjectId(), true, PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.tasks.size).isEqualTo(2)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("B", "C")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all tasks for cr not role based`() {
    setAuthentication(getIdentifier("userCr"))

    val response =
        taskController.findAll(
            getIdentifier("project").asProjectId(), false, PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.tasks.size).isEqualTo(3)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("A", "B", "C")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all tasks for fm`() {
    setAuthentication(getIdentifier("userFm"))

    val response =
        taskController.findAll(
            getIdentifier("project").asProjectId(), true, PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.tasks.size).isEqualTo(1)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("C")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all tasks for fm not role based`() {
    setAuthentication(getIdentifier("userFm"))

    val response =
        taskController.findAll(
            getIdentifier("project").asProjectId(), false, PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.tasks.size).isEqualTo(3)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("A", "B", "C")

    projectEventStoreUtils.verifyEmpty()
  }
}
