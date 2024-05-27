/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.DeleteProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SearchProjectListResource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable

@EnableAllKafkaListeners
class ProjectSearchIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectSearchController
  @Autowired private lateinit var projectController: ProjectController

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val projectNew by lazy {
    repositories.findProject(getIdentifier("projectNew").asProjectId())!!
  }

  private val company1 by lazy { repositories.findCompany(getIdentifier("company"))!! }
  private val company2 by lazy { repositories.findCompany(getIdentifier("comp2"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify search projects after participant changes the company`() {
    eventStreamGenerator.submitParticipantG3(
        asReference = "participantCsm1", eventType = ParticipantEventEnumAvro.DEACTIVATED) {
          it.status = ParticipantStatusEnumAvro.INACTIVE
          it.project = getByReference("project")
        }

    assertThatParticipantIsDeactivated(getIdentifier("project").asProjectId())

    eventStreamGenerator.submitEmployee(
        asReference = "employeeCsm1", eventType = EmployeeEventEnumAvro.DELETED)

    assertThatEmployeeIsDeleted("employeeCsm1")

    eventStreamGenerator
        .setUserContext(name = "system")
        .submitCompany(asReference = "comp2")
        .submitEmployee(asReference = "employeeOther") {
          it.user = getByReference("userCsm1")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
          it.company = getByReference("comp2")
        }
        .setUserContext(name = "userCsm1")
        .submitProject(asReference = "projectNew") {
          it.title = "cProject"
          it.category = ProjectCategoryEnumAvro.OB
          it.client = "Client"
          it.description = "Description"
        }
        .submitParticipantG3(asReference = "participantOther") {
          it.company = getByReference("comp2")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    setAuthentication("admin")
    val searchResource = SearchProjectListResource(null, null, "Daniel")

    val response = cut.search(searchResource, Pageable.ofSize(10)).body!!

    assertThat(response.projects).hasSize(2)

    assertThat(response.projects)
        .extracting("title", "createdBy.displayName", "company.displayName")
        .containsExactlyInAnyOrder(
            tuple(project.title, "Daniel Düsentrieb", company1.name),
            tuple(projectNew.title, "Daniel Düsentrieb", company2.name))
  }

  @Test
  fun `verify search projects having no filter parameters`() {
    eventStreamGenerator
        .setUserContext(name = "system")
        .submitCompany(asReference = "comp2")
        .submitEmployee(asReference = "employeeOther") {
          it.user = getByReference("userCsm1")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
          it.company = getByReference("comp2")
        }
        .setUserContext(name = "userCsm1")
        .submitProject(asReference = "projectNew") {
          it.title = "cProject"
          it.category = ProjectCategoryEnumAvro.OB
          it.client = "Client"
          it.description = "Description"
        }
        .submitParticipantG3(asReference = "participantOther") {
          it.company = getByReference("comp2")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    setAuthentication("admin")

    val searchResource = SearchProjectListResource(null, null, null)

    val response = cut.search(searchResource, Pageable.ofSize(20)).body!!

    assertThat(response.projects)
        .hasSize(2)
        .extracting("title")
        .containsExactlyInAnyOrder(project.title, projectNew.title)
  }

  @Test
  fun `verify search projects and hide deleted projects`() {

    eventStreamGenerator
        .setUserContext(name = "system")
        .submitCompany(asReference = "comp2")
        .submitEmployee(asReference = "employeeOther") {
          it.user = getByReference("userCsm1")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
          it.company = getByReference("comp2")
        }
        .setUserContext(name = "userCsm1")
        .submitProject(asReference = "projectNew") {
          it.title = "cProject"
          it.category = ProjectCategoryEnumAvro.OB
          it.client = "Client"
          it.description = "Description"
        }
        .submitParticipantG3(asReference = "participantOther") {
          it.company = getByReference("comp2")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    setAuthentication("admin")
    projectController.deleteProject(project.identifier, DeleteProjectResource(project.title))

    val searchResource = SearchProjectListResource("", "", "")
    val response = cut.search(searchResource, Pageable.ofSize(20)).body!!

    assertThat(response.projects)
        .hasSize(1)
        .extracting("title")
        .containsExactlyInAnyOrder(projectNew.title)
  }

  private fun assertThatEmployeeIsDeleted(employeeIdentifier: String) {
    assertThat(
            repositories.employeeRepository.findOneByIdentifier(getIdentifier(employeeIdentifier)))
        .isNull()
  }

  private fun assertThatParticipantIsDeactivated(
      projectIdentifier: ProjectId = getIdentifier("project").asProjectId()
  ) {
    repositories.participantRepository
        .findOneByUserIdentifierAndProjectIdentifier(getIdentifier("userCsm1"), projectIdentifier)!!
        .apply {
          assertThat(this).isNotNull
          assertThat(this.status).isEqualTo(ParticipantStatusEnum.INACTIVE)
        }
  }
}
