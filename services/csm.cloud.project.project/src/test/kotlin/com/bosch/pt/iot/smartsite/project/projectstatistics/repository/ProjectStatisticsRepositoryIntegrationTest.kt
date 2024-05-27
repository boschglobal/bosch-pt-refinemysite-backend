/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectstatistics.repository

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.iot.smartsite.ProjectApplicationRepositoryTest
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.CompanyBuilder.Companion.company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeBuilder.Companion.employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ParticipantBuilder.Companion.participant
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftBuilder.Companion.projectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.TaskStatusStatisticsEntry
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.TopicCriticalityStatisticsEntry
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder.Companion.task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicBuilder.Companion.topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import java.util.LinkedList
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder

class ProjectStatisticsRepositoryIntegrationTest : ProjectApplicationRepositoryTest() {

  @Autowired private lateinit var userRepository: UserRepository

  @Autowired private lateinit var projectCraftRepository: ProjectCraftRepository

  @Autowired private lateinit var employeeRepository: EmployeeRepository

  @Autowired private lateinit var companyRepository: CompanyRepository

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var taskRepository: TaskRepository

  @Autowired private lateinit var topicRepository: TopicRepository

  @Autowired private lateinit var participantRepository: ParticipantRepository

  @Autowired private lateinit var projectStatisticsRepository: ProjectStatisticsRepository

  private val userList: MutableList<User> = LinkedList()
  private val companyList: MutableList<Company> = LinkedList()
  private val employeeList: MutableList<Employee> = LinkedList()
  private val projectList: MutableList<Project> = LinkedList()
  private val participantList: MutableList<Participant> = LinkedList()
  private val projectCraftList: MutableList<ProjectCraft> = LinkedList()
  private val taskList: MutableList<Task> = LinkedList()

  @BeforeEach
  fun init() {

    SecurityContextHolder.clearContext()
    userList.add(createUser(randomUUID().toString()))
    userList.add(createUser(randomUUID().toString()))
    userList.add(createUser(randomUUID().toString()))

    val companyUuidList: List<UUID> = listOf(randomUUID(), randomUUID())
    val projectUuidList: List<ProjectId> = listOf(ProjectId(), ProjectId())

    companyList.add(createCompany(companyUuidList[0], "company0", userList[0]))
    companyList.add(createCompany(companyUuidList[1], "Company1", userList[0]))

    employeeList.add(createEmployee(userList[0], companyList[0], EmployeeRoleEnum.CSM))
    employeeList.add(createEmployee(userList[1], companyList[1], EmployeeRoleEnum.CR))
    employeeList.add(createEmployee(userList[2], companyList[1], EmployeeRoleEnum.FM))

    projectList.add(createProject(projectUuidList[0], userList[0].identifier!!.asUserId()))
    projectList.add(createProject(projectUuidList[1], userList[0].identifier!!.asUserId()))

    participantList.add(createParticipant(projectList[0], employeeList[0], CSM, userList[0]))
    participantList.add(createParticipant(projectList[1], employeeList[1], CR, userList[1]))
    participantList.add(createParticipant(projectList[1], employeeList[2], FM, userList[2]))

    projectCraftList.add(createProjectCraft(randomUUID(), "craft0", projectList[0], userList[0]))
    projectCraftList.add(createProjectCraft(randomUUID(), "craft1", projectList[1], userList[0]))

    taskList.add(createTask(projectList[0], DRAFT, projectCraftList[0], null, userList[0]))
    taskList.add(createTask(projectList[0], DRAFT, projectCraftList[0], null, userList[0]))
    taskList.add(createTask(projectList[0], OPEN, projectCraftList[0], null, userList[0]))
    taskList.add(
        createTask(projectList[0], OPEN, projectCraftList[0], participantList[0], userList[0]))
    taskList.add(
        createTask(projectList[0], STARTED, projectCraftList[0], participantList[0], userList[0]))
    taskList.add(createTask(projectList[0], STARTED, projectCraftList[0], null, userList[0]))
    taskList.add(createTask(projectList[0], CLOSED, projectCraftList[0], null, userList[0]))
    taskList.add(createTask(projectList[0], ACCEPTED, projectCraftList[0], null, userList[0]))

    createTopic(taskList[2], CRITICAL, userList[0])
    createTopic(taskList[2], UNCRITICAL, userList[0])
    createTopic(taskList[6], UNCRITICAL, userList[0])

    taskList.add(createTask(projectList[1], DRAFT, projectCraftList[1], null, userList[0]))
    taskList.add(
        createTask(projectList[1], STARTED, projectCraftList[1], participantList[1], userList[0]))
    taskList.add(
        createTask(projectList[1], STARTED, projectCraftList[1], participantList[1], userList[0]))
    taskList.add(
        createTask(projectList[1], STARTED, projectCraftList[1], participantList[2], userList[0]))
    taskList.add(
        createTask(projectList[1], STARTED, projectCraftList[1], participantList[2], userList[0]))
    taskList.add(
        createTask(projectList[1], CLOSED, projectCraftList[1], participantList[2], userList[0]))

    val taskToDelete =
        createTask(projectList[1], CLOSED, projectCraftList[1], participantList[2], userList[0])
    taskList.add(taskToDelete)
    taskRepository.markAsDeleted(taskToDelete.id!!)

    createTopic(taskList[8], UNCRITICAL, userList[0])
    createTopic(taskList[9], UNCRITICAL, userList[0])
    createTopic(taskList[10], CRITICAL, userList[0])

    val topicToDelete = createTopic(taskList[10], CRITICAL, userList[0])
    topicRepository.markAsDeleted(topicToDelete.id!!)
  }

  @AfterEach fun clearSecurityContext() = SecurityContextHolder.clearContext()

  @Test
  fun `verify get project task statistics`() {
    val projectIdentifier = projectList[0].identifier
    val taskStatistics = projectStatisticsRepository.getTaskStatistics(setOf(projectIdentifier))

    assertThat(taskStatistics)
        .contains(
            TaskStatusStatisticsEntry(2L, DRAFT, projectIdentifier),
            TaskStatusStatisticsEntry(2L, OPEN, projectIdentifier),
            TaskStatusStatisticsEntry(2L, STARTED, projectIdentifier),
            TaskStatusStatisticsEntry(1L, CLOSED, projectIdentifier),
            TaskStatusStatisticsEntry(1L, ACCEPTED, projectIdentifier))
  }

  @Test
  fun `verify get project topic statistics`() {
    val projectIdentifier = projectList[0].identifier
    val topicStatistics = projectStatisticsRepository.getTopicStatistics(setOf(projectIdentifier))

    assertThat(topicStatistics)
        .contains(
            TopicCriticalityStatisticsEntry(2L, UNCRITICAL, projectIdentifier),
            TopicCriticalityStatisticsEntry(1L, CRITICAL, projectIdentifier))
  }

  @Test
  fun `verify get project statistics for unknown project`() {
    val taskStatistics = projectStatisticsRepository.getTaskStatistics(setOf(ProjectId()))
    val topicStatistics = projectStatisticsRepository.getTopicStatistics(setOf(ProjectId()))

    assertThat(taskStatistics).isEmpty()
    assertThat(topicStatistics).isEmpty()
  }

  private fun createUser(userId: String): User =
      userRepository.save(user().withUserId(userId).withIdentifier(randomUUID()).build())

  private fun createCompany(uuid: UUID, companyName: String, user: User): Company =
      companyRepository.save(
          company()
              .withIdentifier(uuid)
              .withName(companyName)
              .withCreatedBy(user)
              .withLastModifiedBy(user)
              .build())

  private fun createEmployee(
      user: User,
      company: Company,
      employeeRoleEnum: EmployeeRoleEnum
  ): Employee =
      employeeRepository.save(
          employee()
              .withUser(user)
              .withCompany(company)
              .withRole(employeeRoleEnum)
              .withCreatedBy(user)
              .withLastModifiedBy(user)
              .withIdentifier(randomUUID())
              .build())

  private fun createProject(projectRef: ProjectId, user: UserId): Project =
      projectRepository.save(
          ProjectBuilder.project()
              .withCreatedBy(user)
              .withLastModifiedBy(user)
              .withIdentifier(projectRef)
              .build())

  private fun createParticipant(
      project: Project,
      employee: Employee,
      role: ParticipantRoleEnum,
      user: User
  ): Participant =
      participantRepository.save(
          participant()
              .withProject(project)
              .withEmployee(employee)
              .withRole(role)
              .withLastModifiedBy(user)
              .withCreatedBy(user)
              .build())

  private fun createProjectCraft(
      uuid: UUID,
      name: String,
      project: Project,
      user: User
  ): ProjectCraft =
      projectCraftRepository.save(
          projectCraft()
              .withIdentifier(uuid.asProjectCraftId())
              .withName(name)
              .withColor("#FFFFFF")
              .withProject(project)
              .withLastModifiedBy(user.getAuditUserId())
              .withCreatedBy(user.getAuditUserId())
              .build())

  private fun createTask(
      project: Project,
      status: TaskStatusEnum,
      projectCraft: ProjectCraft,
      assignee: Participant?,
      creator: User
  ): Task =
      taskRepository.save(
          task()
              .withIdentifier(TaskId())
              .withProject(project)
              .withStatus(status)
              .withAssignee(assignee)
              .withProjectCraft(projectCraft)
              .withCreatedBy(creator)
              .withLastModifiedBy(creator)
              .build())

  private fun createTopic(task: Task, criticality: TopicCriticalityEnum, creator: User): Topic =
      topicRepository.save(
          topic()
              .withIdentifier(randomUUID())
              .withTask(task)
              .withCriticality(criticality)
              .withCreatedBy(creator)
              .withLastModifiedBy(creator)
              .build())
}
