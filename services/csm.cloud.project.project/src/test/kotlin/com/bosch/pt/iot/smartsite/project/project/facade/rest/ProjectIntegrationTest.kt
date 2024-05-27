/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_DELETE_VALIDATION_ERROR_PROJECT_TITLE_INCORRECT
import com.bosch.pt.iot.smartsite.common.kafka.AggregateIdentifierUtils.getAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_REQUEST
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.DeleteProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.OB
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.util.withMessageKey
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity

@DisplayName("Test project controller edge cases")
@EnableAllKafkaListeners
class ProjectIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectController

  @Autowired private lateinit var projectRepository: ProjectRepository

  @MockkBean(relaxed = true) private lateinit var commandSendingService: CommandSendingService

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val csmUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify project cannot be created when start date is after end date`() {
    doWithAuthorization(csmUser) {
      val startDate = LocalDate.now()

      val createProjectResource =
          SaveProjectResource(
              client = "client",
              description = "description",
              start = startDate,
              end = startDate.minus(1, DAYS),
              projectNumber = "projectNumber",
              title = "title",
              category = OB,
              address = ProjectAddressDto("city", "HN", "street", "ZC"))

      assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
        cut.createProject(null, createProjectResource)
      }
    }
  }

  @Test
  fun `verify project cannot be created when csm unknown`() {
    doWithAuthorization(csmUser) {
      val startDate = LocalDate.now()

      val createProjectResource =
          SaveProjectResource(
              client = "client",
              description = "description",
              start = startDate,
              end = startDate.minus(1, DAYS),
              projectNumber = "projectNumber",
              title = "title",
              category = OB,
              address = ProjectAddressDto("city", "HN", "street", "ZC"),
          )

      assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
        cut.createProject(null, createProjectResource)
      }
    }
  }

  @Test
  fun `verify project list is empty if user is not yet assigned to a company`() {
    eventStreamGenerator.submitUser("userNotAssignedToCompany")
    setAuthentication("userNotAssignedToCompany")

    val response = cut.findAllProjects(DEFAULT_PAGE_REQUEST)
    assertThat(response.statusCode).isEqualTo(OK)

    val projectListResource = response.body!!
    assertThat(projectListResource).isNotNull
    assertThat(projectListResource.projects).isEmpty()
    assertThat(projectListResource.userActivated).isFalse
  }

  @Test
  fun `verify project list contains correct companies of the project's creator`() {
    eventStreamGenerator
        .submitUserAndActivate("user1")
        .submitUser("user2")
        .submitUser("user3")
        .submitUser("user4")

    setAuthentication("user1")

    eventStreamGenerator
        .submitCompany("company1")
        .submitCompany("company2")
        .submitEmployee("employee1") {
          it.company = getByReference("company1")
          it.user = getByReference("user1")
        }
        .submitEmployee("employee2") {
          it.company = getByReference("company1")
          it.user = getByReference("user2")
        }
        .submitEmployee("employee3") {
          it.company = getByReference("company2")
          it.user = getByReference("user3")
        }
        .submitEmployee("employee4") {
          it.company = getByReference("company2")
          it.user = getByReference("user4")
        }

    // Scenario 1 - There is one CSM in the project
    eventStreamGenerator
        .submitProject("project1")
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user2")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user3")
          it.role = ParticipantRoleEnumAvro.CR
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user4")
          it.role = ParticipantRoleEnumAvro.FM
        }
    val project1 = repositories.findProject(getIdentifier("project1").asProjectId())!!

    // Scenario 2 - The initial CSM was deactivated, a new CSM of another company is active
    eventStreamGenerator
        .submitProject("project2")
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user1")
          it.role = ParticipantRoleEnumAvro.CSM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user2")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user3")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user4")
          it.role = ParticipantRoleEnumAvro.FM
        }
    val project2 = repositories.findProject(getIdentifier("project2").asProjectId())!!

    // Scenario 3 - There are multiple CSMs in the project. The initial CSM was deactivated, CSM of
    // company 2 was created next, then a CSM of company 1 was added
    eventStreamGenerator
        .submitProject("project3")
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user1")
          it.role = ParticipantRoleEnumAvro.CSM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user2")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user3")
          it.role = ParticipantRoleEnumAvro.CR
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user4")
          it.role = ParticipantRoleEnumAvro.CSM
        }
    val project3 = repositories.findProject(getIdentifier("project3").asProjectId())!!

    // Scenario 4 - The creation order is incorrect (shouldn't happen) - but the query returns the
    // company of the first CSM
    eventStreamGenerator
        .submitProject("project4")
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user1")
          it.role = ParticipantRoleEnumAvro.CR
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user2")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user3")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user4")
          it.role = ParticipantRoleEnumAvro.FM
        }
    val project4 = repositories.findProject(getIdentifier("project4").asProjectId())!!

    // Scenario 5 - No CSM is found (shouldn't happen)
    eventStreamGenerator
        .submitProject("project5")
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user1")
          it.role = ParticipantRoleEnumAvro.CR
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company1")
          it.user = getByReference("user2")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user3")
          it.role = ParticipantRoleEnumAvro.CR
        }
        .submitParticipantG3(randomString()) {
          it.company = getByReference("company2")
          it.user = getByReference("user4")
          it.role = ParticipantRoleEnumAvro.FM
        }
    val project5 = repositories.findProject(getIdentifier("project5").asProjectId())!!

    val company1 = repositories.findCompany(getIdentifier("company1"))!!
    val company2 = repositories.findCompany(getIdentifier("company2"))!!

    // Authorize with a user that is still active in all projects
    setAuthentication("user4")
    val response = cut.findAllProjects(DEFAULT_PAGE_REQUEST)
    assertThat(response.statusCode).isEqualTo(OK)

    val projectListResource = response.body!!
    assertThat(projectListResource).isNotNull
    assertThat(projectListResource.projects).hasSize(5)

    val projects: List<ProjectResource> = ArrayList(projectListResource.projects)

    // Validate scenario 1
    val projectResource1 = getResource(projects, project1.identifier.toUuid())
    assertThat(projectResource1.company!!.identifier).isEqualTo(company1.identifier)
    assertThat(projectResource1.company!!.displayName).isEqualTo(company1.name)

    // Validate scenario 2
    val projectResource2 = getResource(projects, project2.identifier.toUuid())
    assertThat(projectResource2.company!!.identifier).isEqualTo(company1.identifier)
    assertThat(projectResource2.company!!.displayName).isEqualTo(company1.name)

    // Validate scenario 3
    val projectResource3 = getResource(projects, project3.identifier.toUuid())
    assertThat(projectResource3.company!!.identifier).isEqualTo(company1.identifier)
    assertThat(projectResource3.company!!.displayName).isEqualTo(company1.name)

    // Validate scenario 4
    val projectResource4 = getResource(projects, project4.identifier.toUuid())
    assertThat(projectResource4.company!!.identifier).isEqualTo(company2.identifier)
    assertThat(projectResource4.company!!.displayName).isEqualTo(company2.name)

    // Validate scenario 5
    val projectResource5 = getResource(projects, project5.identifier.toUuid())
    assertThat(projectResource5.company).isNull()
  }

  @Test
  fun deleteProject() {
    val deleteResource = DeleteProjectResource(project.title)

    val response: ResponseEntity<*> = cut.deleteProject(project.identifier, deleteResource)
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)

    assertThat(projectRepository.findOneByIdentifier(project.identifier)!!.deleted).isTrue

    val expectedKey = CommandMessageKey(project.identifier.toUuid())
    val expectedValue = buildDeleteCommand(project)
    verify { commandSendingService.send(expectedKey, expectedValue, any()) }
  }

  @Test
  fun deleteProjectFailsWhenProjectTitleDoesNotMatch() {
    val deleteResource = DeleteProjectResource(randomAlphabetic(10))

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.deleteProject(project.identifier, deleteResource) }
        .withMessageKey(PROJECT_DELETE_VALIDATION_ERROR_PROJECT_TITLE_INCORRECT)
    assertThat(projectRepository.findOneByIdentifier(project.identifier)!!.deleted).isFalse
  }

  private fun buildDeleteCommand(project: Project): DeleteCommandAvro =
      DeleteCommandAvro(
          getAggregateIdentifier(project, PROJECT.value),
          getAggregateIdentifier(SecurityContextHelper.getInstance().getCurrentUser(), USER.value))

  private fun getResource(projects: List<ProjectResource>, identifier: UUID): ProjectResource =
      projects.firstOrNull { p: ProjectResource -> p.id == identifier }
          ?: throw IllegalStateException("Project with identifier $identifier not found")
}
