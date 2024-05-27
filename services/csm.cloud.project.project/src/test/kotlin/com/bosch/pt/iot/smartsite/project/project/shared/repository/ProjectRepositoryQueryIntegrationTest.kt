/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.repository

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.iot.smartsite.ProjectApplicationRepositoryTest
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_REQUEST
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.CompanyBuilder.Companion.company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeBuilder.Companion.employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum
import com.bosch.pt.iot.smartsite.company.model.StreetAddressBuilder.Companion.streetAddress
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ParticipantBuilder.Companion.participant
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaListBuilder.Companion.workAreaList
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.IdGenerator
import org.springframework.util.SimpleIdGenerator

@TestPropertySource(
    properties =
        [
            "spring.jpa.hibernate.show-sql=true",
            "logging.level.org.hibernate.type.descriptor.sql=trace"])
@SpringJUnitConfig(classes = [ProjectRepositoryQueryIntegrationTest.TestConfig::class])
internal class ProjectRepositoryQueryIntegrationTest : ProjectApplicationRepositoryTest() {

  @Autowired private lateinit var testEntityManager: TestEntityManager

  @Autowired private lateinit var cut: ProjectRepository

  @Autowired private lateinit var idGenerator: IdGenerator

  private lateinit var creator: User

  @BeforeEach
  fun init() {
    SecurityContextHolder.clearContext()
    creator = testEntityManager.persist(user().withUserId("creator").build())
  }

  @AfterEach fun cleanup() = SecurityContextHolder.clearContext()

  @Test
  fun verifyFindAllProjectsForCurrentUserCsmAsCsm() {
    val csmUser = testEntityManager.persist(user().withUserId("csm").build())
    createAuthentication(csmUser)

    val company1 = createCompany()
    val employeeCsm = createEmployee(csmUser, company1, EmployeeRoleEnum.CSM)

    val projectA = createProject(employeeCsm)
    val projectB = createProject(employeeCsm)

    val projects = cut.findAllWhereCurrentUserIsActiveParticipant(DEFAULT_PAGE_REQUEST).content

    assertThat(projects).hasSize(2)
    assertThat(projects).contains(projectA, projectB)
  }

  @Test
  fun verifyFindAllProjectsForCurrentUserFmAsFm() {
    val csmUser = testEntityManager.persist(user().withUserId("csm").build())
    val fmUser = testEntityManager.persist(user().withUserId("fm").build())
    createAuthentication(fmUser)

    val company1 = createCompany()
    val employeeCsm = createEmployee(csmUser, company1, EmployeeRoleEnum.CSM)
    val employeeFm = createEmployee(fmUser, company1, EmployeeRoleEnum.FM)

    val projectA = createProject(employeeCsm)
    createProject(employeeCsm)
    createParticipant(employeeFm, projectA, FM)

    val projects = cut.findAllWhereCurrentUserIsActiveParticipant(DEFAULT_PAGE_REQUEST).content

    assertThat(projects).hasSize(1)
    assertThat(projects).contains(projectA)
  }

  @Test
  fun verifyFindAllProjectsForCurrentUserCrAsCr() {
    val csmUser = testEntityManager.persist(user().withUserId("csm").build())
    val crUser = testEntityManager.persist(user().withUserId("cr").build())
    createAuthentication(crUser)

    val company1 = createCompany()
    val employeeCsm = createEmployee(csmUser, company1, EmployeeRoleEnum.CSM)
    val employeeCr = createEmployee(crUser, company1, EmployeeRoleEnum.CR)

    val projectA = createProject(employeeCsm)
    createProject(employeeCsm)
    createParticipant(employeeCr, projectA, CR)

    val projects = cut.findAllWhereCurrentUserIsActiveParticipant(DEFAULT_PAGE_REQUEST).content

    assertThat(projects).hasSize(1)
    assertThat(projects).contains(projectA)
  }

  @Test
  fun verifyHideDeletedProjects() {
    val csmUser = testEntityManager.persist(user().withUserId("csm").build())
    createAuthentication(csmUser)

    val company1 = createCompany()
    val employeeCsm = createEmployee(csmUser, company1, EmployeeRoleEnum.CSM)

    val projectA = createProject(employeeCsm)
    val projectB = createProject(employeeCsm)
    val projectC = createProject(employeeCsm)

    var projects = cut.findAllWhereCurrentUserIsActiveParticipant(DEFAULT_PAGE_REQUEST).content

    assertThat(projects).hasSize(3)
    assertThat(projects).contains(projectA, projectB, projectC)

    cut.markAsDeleted(projectC.id!!)

    projects = cut.findAllWhereCurrentUserIsActiveParticipant(DEFAULT_PAGE_REQUEST).content
    assertThat(projects).hasSize(2)
    assertThat(projects).contains(projectA, projectB)

    projects = cut.findAllWithDetails()
    assertThat(projects).hasSize(2)
    assertThat(projects).contains(projectA, projectB)
  }

  private fun createParticipant(employee: Employee, project: Project, role: ParticipantRoleEnum) {
    testEntityManager.persist(
        participant()
            .withIdentifier(idGenerator.generateId())
            .withEmployee(employee)
            .withProject(project)
            .withRole(role)
            .withCreatedBy(creator)
            .withLastModifiedBy(creator)
            .build())
  }

  private fun createProject(csm: Employee): Project {
    var project =
        project()
            .withIdentifier(ProjectId())
            .withCreatedBy(creator.identifier!!.asUserId())
            .withLastModifiedBy(creator.identifier!!.asUserId())
            .build()

    val workAreaList =
        workAreaList()
            .withIdentifier(WorkAreaListId())
            .withProject(project)
            .withCreatedBy(creator)
            .withLastModifiedBy(creator)
            .build()

    project = testEntityManager.persist(project)
    testEntityManager.persist(workAreaList)
    createParticipant(csm, project, CSM)

    return project
  }

  private fun createEmployee(user: User, company: Company, role: EmployeeRoleEnum): Employee =
      testEntityManager.persist(
          employee()
              .withIdentifier(idGenerator.generateId())
              .withCompany(company)
              .withRole(role)
              .withUser(user)
              .withCreatedBy(creator)
              .withLastModifiedBy(creator)
              .build())

  private fun createCompany(): Company =
      testEntityManager.persist(
          company()
              .withIdentifier(idGenerator.generateId())
              .withName("company 1")
              .withStreetAddress(
                  streetAddress()
                      .withCity("City")
                      .withCountry("Country")
                      .withStreet("Street")
                      .withHouseNumber("10")
                      .withZipCode("12345")
                      .build())
              .withCreatedBy(creator)
              .withLastModifiedBy(creator)
              .build())

  private fun createAuthentication(user: User) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, "n/a", createAuthorityList("ROL_USER"))
  }

  /** Configures security evaluation context for tests. */
  @TestConfiguration
  open class TestConfig {

    @Bean
    open fun securityEvaluationContextExtension(): SecurityEvaluationContextExtension =
        SecurityEvaluationContextExtension()

    @Bean open fun idGenerator(): IdGenerator = SimpleIdGenerator()
  }
}
