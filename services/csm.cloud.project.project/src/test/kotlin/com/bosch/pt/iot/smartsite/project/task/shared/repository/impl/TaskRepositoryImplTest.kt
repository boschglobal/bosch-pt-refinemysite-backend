/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.repository.impl

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
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftBuilder.Companion.projectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftListBuilder.Companion.projectCraftList
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder.Companion.task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.COMPANY
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.END
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.LOCATION
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.NAME
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.PROJECT_CRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.START
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.STATUS
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.TOPIC
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.WORK_AREA
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskScheduleBuilder.Companion.taskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicBuilder.Companion.topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaBuilder.Companion.workArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaListBuilder.Companion.workAreaList
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE
import java.time.LocalDate
import java.util.LinkedList
import java.util.Locale.ENGLISH
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class TaskRepositoryImplTest : ProjectApplicationRepositoryTest() {

  @Autowired private lateinit var userRepository: UserRepository

  @Autowired private lateinit var projectCraftRepository: ProjectCraftRepository

  @Autowired private lateinit var projectCraftListRepository: ProjectCraftListRepository

  @Autowired private lateinit var workAreaRepository: WorkAreaRepository

  @Autowired private lateinit var workAreaListRepository: WorkAreaListRepository

  @Autowired private lateinit var employeeRepository: EmployeeRepository

  @Autowired private lateinit var companyRepository: CompanyRepository

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var participantRepository: ParticipantRepository

  @Autowired private lateinit var testEntityManager: TestEntityManager

  // Components under test
  @Autowired private lateinit var taskRepository: TaskRepository

  @Autowired private lateinit var taskScheduleRepository: TaskScheduleRepository

  /** Set up the data before every te\t method. */
  @BeforeEach
  fun setUpDataBeforeTest() {
    SecurityContextHolder.clearContext()
  }

  /** Removes the data after every test method. */
  @AfterEach
  fun removeDataAfterTest() {
    SecurityContextHolder.clearContext()
    testEntityManager.clear()
    testEntityManager.flush()
  }

  /** Verifies that filtering tasks work for the status. */
  @Test
  fun verifyFindAllWithHasStatus() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(projectRef = project.identifier, taskStatus = listOf(DRAFT, CLOSED))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(2)
    assertThat(tasks).contains(taskList[0].identifier, taskList[2].identifier)
    assertThat(tasks).doesNotContain(taskList[1].identifier)
  }

  /** Verifies that filtering tasks work for the status with empty list. */
  @Test
  fun verifyFindAllWithHasStatusEmpty() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    taskRepository.save(
        task()
            .withName("task0")
            .withIdentifier(TaskId())
            .withProject(project)
            .withStatus(DRAFT)
            .withoutAssignee()
            .withCreatedBy(user)
            .withProjectCraft(projectCraft)
            .withLastModifiedBy(user)
            .withLocation("TaskLocation0")
            .build())
    taskRepository.save(
        task()
            .withName("Task1")
            .withIdentifier(TaskId())
            .withProject(project)
            .withStatus(OPEN)
            .withAssignee(participant)
            .withCreatedBy(user)
            .withProjectCraft(projectCraft)
            .withLastModifiedBy(user)
            .withLocation("TaskLocation1")
            .build())
    taskRepository.save(
        task()
            .withName("Task2")
            .withIdentifier(TaskId())
            .withProject(project)
            .withStatus(CLOSED)
            .withAssignee(participant)
            .withCreatedBy(user)
            .withProjectCraft(projectCraft)
            .withLastModifiedBy(user)
            .withLocation("TaskLocation2")
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto = TaskFilterDto(projectRef = project.identifier, taskStatus = emptyList())

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(0)
  }

  /** Verifies that filtering tasks work for the status with a null list. */
  @Test
  fun verifyFindAllWithHasStatusNull() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    taskRepository.save(
        task()
            .withName("task0")
            .withIdentifier(TaskId())
            .withProject(project)
            .withStatus(DRAFT)
            .withoutAssignee()
            .withCreatedBy(user)
            .withProjectCraft(projectCraft)
            .withLastModifiedBy(user)
            .withLocation("TaskLocation0")
            .build())
    taskRepository.save(
        task()
            .withName("Task1")
            .withIdentifier(TaskId())
            .withProject(project)
            .withStatus(OPEN)
            .withAssignee(participant)
            .withCreatedBy(user)
            .withProjectCraft(projectCraft)
            .withLastModifiedBy(user)
            .withLocation("TaskLocation1")
            .build())
    taskRepository.save(
        task()
            .withName("Task2")
            .withIdentifier(TaskId())
            .withProject(project)
            .withStatus(CLOSED)
            .withAssignee(participant)
            .withCreatedBy(user)
            .withProjectCraft(projectCraft)
            .withLastModifiedBy(user)
            .withLocation("TaskLocation2")
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto = TaskFilterDto(projectRef = project.identifier, taskStatus = null)

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(0)
  }

  /** Verifies that filtering tasks with topics works. */
  @Test
  fun verifyFindAllWithTopics() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))
    val topicA = buildTopic(CRITICAL, user)
    val topicB = buildTopic(UNCRITICAL, user)

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(topicA)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(topicB)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            hasTopics = TRUE)

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(2)
    assertThat(tasks).contains(taskList[1].identifier, taskList[2].identifier)
    assertThat(tasks).doesNotContain(taskList[0].identifier)
  }

  /** Verifies that filtering tasks with no topics works. */
  @Test
  fun verifyFindAllWithoutTopics() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))
    val topicA = buildTopic(CRITICAL, user)
    val topicB = buildTopic(UNCRITICAL, user)

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(topicA)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(topicB)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            hasTopics = FALSE)

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(1)
    assertThat(tasks).contains(taskList[0].identifier)
    assertThat(tasks).doesNotContain(taskList[1].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks with criticality topics works. */
  @Test
  fun verifyFindAllWithHasTopicCriticality() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))
    val topicA = buildTopic(CRITICAL, user)
    val topicB = buildTopic(UNCRITICAL, user)

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(topicA)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(topicB)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            topicCriticality = listOf(CRITICAL))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, TOPIC.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(1)
    assertThat(tasks).contains(taskList[1].identifier)
    assertThat(tasks).doesNotContain(taskList[0].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks with projectcraft id works. */
  @Test
  fun verifyFindAllWithHasCraftIds() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraftA =
        createProjectCraft(randomUUID(), "craftA", project, projectCraftList, 0, user)
    val projectCraftB =
        createProjectCraft(randomUUID(), "craftB", project, projectCraftList, 1, user)
    val projectCraftC =
        createProjectCraft(randomUUID(), "craftC0", project, projectCraftList, 2, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraftA, projectCraftB, projectCraftC))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraftA)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraftB)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraftC)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            projectCraftIds = listOf(projectCraftA.identifier, projectCraftC.identifier))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(2)
    assertThat(tasks).contains(taskList[0].identifier, taskList[2].identifier)
    assertThat(tasks).doesNotContain(taskList[1].identifier)
  }

  /** Verifies that filtering tasks with project id and participant works. */
  @Test
  fun verifyFindAllWithHasProjectId() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val projectA = createProject(ProjectId(), user.identifier!!.asUserId())
    val projectB = createProject(ProjectId(), user.identifier!!.asUserId())
    val participantA = createParticipant(projectA, employee, CSM, user)
    val participantB = createParticipant(projectB, employee, CSM, user)
    val projectCraftListA = createProjectCraftList(projectA, user)
    val projectCraftListB = createProjectCraftList(projectB, user)
    val projectCraftA =
        createProjectCraft(randomUUID(), "craftA", projectA, projectCraftListA, 0, user)
    val projectCraftB =
        createProjectCraft(randomUUID(), "craftB", projectB, projectCraftListB, 0, user)
    projectCraftListA.addProjectCraftToList(listOf(projectCraftA))
    projectCraftListB.addProjectCraftToList(listOf(projectCraftB))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(projectA)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraftA)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(projectA)
                .withStatus(OPEN)
                .withAssignee(participantA)
                .withCreatedBy(user)
                .withProjectCraft(projectCraftA)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(projectB)
                .withStatus(CLOSED)
                .withAssignee(participantB)
                .withCreatedBy(user)
                .withProjectCraft(projectCraftB)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = projectA.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(2)
    assertThat(tasks).contains(taskList[0].identifier, taskList[1].identifier)
    assertThat(tasks).doesNotContain(taskList[2].identifier)
  }

  /** Verifies that filtering tasks with company id works. */
  @Test
  fun verifyFindAllWithHAssignedCompany() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val userA = createUser(randomUUID().toString(), "A1", "A2")
    val userB = createUser(randomUUID().toString(), "B1", "B2")
    val companyA = createCompany(randomUUID(), "companyA", user)
    val companyB = createCompany(randomUUID(), "companyB", user)
    val employeeA = createEmployee(companyA, EmployeeRoleEnum.CSM, userA)
    val employeeB = createEmployee(companyB, EmployeeRoleEnum.CSM, userB)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participantA = createParticipant(project, employeeA, CSM, user)
    val participantB = createParticipant(project, employeeB, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participantA)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participantB)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            assignedCompanies = listOf(companyB.identifier!!))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(1)
    assertThat(tasks).contains(taskList[2].identifier)
    assertThat(tasks).doesNotContain(taskList[0].identifier, taskList[1].identifier)
  }

  /**
   * Verifies that filtering tasks with start date works. Check tasks that start or end after the
   * start date.
   */
  @Test
  fun verifyFindAllWithFromDate() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Create the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[0])
            .withStart(LocalDate.of(2016, 1, 1))
            .withEnd(LocalDate.of(2016, 2, 28))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskScheduleRepository.save(
            taskSchedule()
                .withTask(taskList[2])
                .withStart(LocalDate.of(2016, 5, 1))
                .withEnd(LocalDate.of(2016, 6, 30))
                .withCreatedBy(user)
                .withLastModifiedBy(user)
                .build()))
    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            rangeStartDate = LocalDate.of(2016, 3, 15))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(2)
    assertThat(tasks).contains(taskList[1].identifier, taskList[2].identifier)
    assertThat(tasks).doesNotContain(taskList[0].identifier)
  }

  /**
   * Verifies that filtering tasks with end date works. Check tasks that start or end before the end
   * date.
   */
  @Test
  fun verifyFindAllWithUntilDate() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Create the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[0])
            .withStart(LocalDate.of(2016, 1, 1))
            .withEnd(LocalDate.of(2016, 2, 28))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[2])
            .withStart(LocalDate.of(2016, 5, 1))
            .withEnd(LocalDate.of(2016, 6, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            rangeEndDate = LocalDate.of(2016, 3, 15))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(2)
    assertThat(tasks).contains(taskList[0].identifier, taskList[1].identifier)
    assertThat(tasks).doesNotContain(taskList[2].identifier)
  }

  /**
   * Verifies that filtering tasks with start and end date works. Check tasks that end after the
   * start date or start before the end date.
   */
  @Test
  fun verifyFindAllWithBetweenDates() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Create of the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[0])
            .withStart(LocalDate.of(2016, 1, 1))
            .withEnd(LocalDate.of(2016, 2, 28))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[2])
            .withStart(LocalDate.of(2016, 5, 1))
            .withEnd(LocalDate.of(2016, 5, 1))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            rangeStartDate = LocalDate.of(2016, 1, 15),
            rangeEndDate = LocalDate.of(2016, 5, 15))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .contains(taskList[0].identifier, taskList[1].identifier, taskList[2].identifier)
  }

  /**
   * Verifies that filtering tasks with start and end date works. Check tasks that start before the
   * start date and end after the end date.
   */
  @Test
  fun verifyFindAllWithBetweenDatesAndRangeInsideTaskDates() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Create of the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[0])
            .withStart(LocalDate.of(2016, 1, 1))
            .withEnd(LocalDate.of(2016, 2, 28))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[2])
            .withStart(LocalDate.of(2016, 5, 1))
            .withEnd(LocalDate.of(2016, 6, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            rangeStartDate = LocalDate.of(2016, 3, 10),
            rangeEndDate = LocalDate.of(2016, 4, 10))

    val pageRequest = PageRequest.of(0, 5)
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(1)
    assertThat(tasks).contains(taskList[1].identifier)
    assertThat(tasks).doesNotContain(taskList[0].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks ordered by name works. */
  @Test
  fun verifyFindAllWithFilterAndOrderName() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, NAME.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[2].identifier, taskList[1].identifier, taskList[0].identifier)
  }

  /** Verifies that filtering tasks ordered by location works. */
  @Test
  fun verifyFindAllWithFilterAndOrderLocation() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, LOCATION.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[0].identifier, taskList[1].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks ordered by start date works. */
  @Test
  fun verifyFindAllWithFilterAndOrderStart() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Create the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[2])
            .withStart(LocalDate.of(2016, 5, 1))
            .withEnd(LocalDate.of(2016, 6, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))
    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, START.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[0].identifier, taskList[1].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks ordered by end date works. */
  @Test
  fun verifyFindAllWithFilterAndOrderEnd() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Create the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())

    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[2])
            .withStart(LocalDate.of(2016, 5, 1))
            .withEnd(LocalDate.of(2016, 6, 30))
            .withCreatedBy(user)
            .withLastModifiedBy(user)
            .build())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, END.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[2].identifier, taskList[1].identifier, taskList[0].identifier)
  }

  /** Verifies that filtering tasks ordered by project craft position value works. */
  @Test
  fun verifyFindAllWithFilterAndOrderCraftByPosition() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraftA =
        createProjectCraft(randomUUID(), "craftA", project, projectCraftList, 1, user)
    val projectCraftB =
        createProjectCraft(randomUUID(), "craftB", project, projectCraftList, 2, user)
    val projectCraftC =
        createProjectCraft(randomUUID(), "craftC", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraftC, projectCraftA, projectCraftB))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraftA)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraftB)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraftC)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, PROJECT_CRAFT.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[1].identifier, taskList[0].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks ordered by company name works. */
  @Test
  fun verifyFindAllWithFilterAndOrderCompanyName() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val userA = createUser(randomUUID().toString(), "A1", "A2")
    val userB1 = createUser(randomUUID().toString(), "B1", "B2")
    val userB2 = createUser(randomUUID().toString(), "B2", "B3")
    val companyA = createCompany(randomUUID(), "companyA", user)
    val companyB = createCompany(randomUUID(), "companyB", user)
    val employeeA = createEmployee(companyA, EmployeeRoleEnum.CSM, userA)
    val employeeB1 = createEmployee(companyB, EmployeeRoleEnum.CSM, userB1)
    val employeeB2 = createEmployee(companyB, EmployeeRoleEnum.CSM, userB2)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participantA = createParticipant(project, employeeA, CSM, user)
    val participantB1 = createParticipant(project, employeeB1, CSM, user)
    val participantB2 = createParticipant(project, employeeB2, CR, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withAssignee(participantA)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participantB1)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participantB2)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task3")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation3")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, COMPANY.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(4)
    assertThat(tasks)
        .containsExactly(
            taskList[3].identifier,
            taskList[0].identifier,
            taskList[1].identifier,
            taskList[2].identifier)
  }

  /** Verifies that filtering tasks ordered by status works. */
  @Test
  fun verifyFindAllWithFilterAndOrderStatus() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, STATUS.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[0].identifier, taskList[1].identifier, taskList[2].identifier)
  }

  /** Verifies that filtering tasks ordered by existing topics works. */
  @Test
  fun verifyFindAllWithFilterAndOrderNews() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withTopic(buildTopic(CRITICAL, user))
                .withLocation("TaskLocation2")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task3")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withLocation("TaskLocation3")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task4")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation4")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, TOPIC.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(5)
    assertThat(tasks)
        .containsExactly(
            taskList[2].identifier,
            taskList[0].identifier,
            taskList[1].identifier,
            taskList[3].identifier,
            taskList[4].identifier)
  }

  /**
   * Verifies that filtering tasks ordered by workAreas ascending works and the nulls are put in the
   * end.
   */
  @Test
  fun verifyFindAllOrderWorkAreasAsc() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    // Create the work areas and the work area list, linked then by order and the persist then
    val workArea0 = buildWorkArea(WorkAreaId(), "Work Area 0", 1, project, user)
    val workArea1 = buildWorkArea(WorkAreaId(), "Work Area 1", 2, project, user)
    val workArea2 = buildWorkArea(WorkAreaId(), "Work Area 2", 3, project, user)
    val workArea3 = buildWorkArea(WorkAreaId(), "Work Area 3", 4, project, user)
    val workAreaList = buildWorkAreaList(project, user)
    workAreaList.addWorkArea(0, workArea0)
    workAreaList.addWorkArea(1, workArea1)
    workAreaList.addWorkArea(2, workArea2)
    workAreaList.addWorkArea(3, workArea3)
    workAreaListRepository.save(workAreaList)

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea3)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea2)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea1)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withTopic(buildTopic(CRITICAL, user))
                .withLocation("TaskLocation2")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task3")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea0)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withLocation("TaskLocation3")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task4")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation4")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, WORK_AREA.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(5)
    assertThat(tasks)
        .containsExactly(
            taskList[3].identifier,
            taskList[2].identifier,
            taskList[1].identifier,
            taskList[0].identifier,
            taskList[4].identifier)
  }

  /**
   * Verifies that filtering tasks ordered by workAreas descending works and the nulls are put on
   * top.
   */
  @Test
  fun verifyFindAllOrderWorkAreasDesc() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    // Create the work areas and the work area list, linked then by order and the persist then
    val workArea0 = buildWorkArea(WorkAreaId(), "Work Area 0", 1, project, user)
    val workArea1 = buildWorkArea(WorkAreaId(), "Work Area 1", 2, project, user)
    val workArea2 = buildWorkArea(WorkAreaId(), "Work Area 2", 3, project, user)
    val workArea3 = buildWorkArea(WorkAreaId(), "Work Area 3", 4, project, user)
    val workAreaList = buildWorkAreaList(project, user)
    workAreaList.addWorkArea(0, workArea0)
    workAreaList.addWorkArea(1, workArea1)
    workAreaList.addWorkArea(2, workArea2)
    workAreaList.addWorkArea(3, workArea3)
    workAreaListRepository.save(workAreaList)

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea0)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea1)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea2)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(CRITICAL, user))
                .withTopic(buildTopic(CRITICAL, user))
                .withLocation("TaskLocation2")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task3")
                .withIdentifier(TaskId())
                .withProject(project)
                .withWorkArea(workArea3)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withTopic(buildTopic(UNCRITICAL, user))
                .withLocation("TaskLocation3")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task4")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation4")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, WORK_AREA.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(5)
    assertThat(tasks)
        .containsExactly(
            taskList[4].identifier,
            taskList[3].identifier,
            taskList[2].identifier,
            taskList[1].identifier,
            taskList[0].identifier)
  }

  /** Verifies that filtering tasks with null order works. */
  @Test
  fun verifyFindAllWithFilterAndOrderWithNullValue() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    val employee = createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())
    val participant = createParticipant(project, employee, CSM, user)
    val projectCraftList = createProjectCraftList(project, user)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, user)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(DRAFT)
                .withoutAssignee()
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(CLOSED)
                .withAssignee(participant)
                .withCreatedBy(user)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(user)
                .withLocation("TaskLocation2")
                .build()))

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))
    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(DESC, NAME.property), null))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(3)
    assertThat(tasks)
        .containsExactly(taskList[2].identifier, taskList[1].identifier, taskList[0].identifier)
  }

  /** Verifies that filtering tasks with invalid order works. */
  @Test
  fun verifyFindAllWithFilterAndOrderWithInvalidValue() {

    // Setup of the environment
    val user = createUser(randomUUID().toString(), "first", "last")
    val company = createCompany(randomUUID(), "companyA", user)
    createEmployee(company, EmployeeRoleEnum.CSM, user)
    val project = createProject(ProjectId(), user.identifier!!.asUserId())

    // Setup of the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier, taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED))

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, "InvalidedOrder")))
    assertThrows(InvalidDataAccessApiUsageException::class.java) {
      taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    }
  }

  @Test
  fun verifyFindAllHavingBothScheduleDatesSet() {
    // Setup of the environment
    val system = createUser(randomUUID().toString(), "System", "last")
    val userFm = createUser(randomUUID().toString(), "Csm", "last")
    val userCsm = createUser(randomUUID().toString(), "Fm", "last")
    val companyA = createCompany(randomUUID(), "companyA", system)
    val employeeFm = createEmployee(companyA, EmployeeRoleEnum.FM, userFm)
    val employeeCsm = createEmployee(companyA, EmployeeRoleEnum.CSM, userCsm)
    val project = createProject(ProjectId(), userCsm.identifier!!.asUserId())
    val fm = createParticipant(project, employeeFm, FM, system)
    createParticipant(project, employeeCsm, CSM, system)
    val projectCraftList = createProjectCraftList(project, userCsm)
    val projectCraft =
        createProjectCraft(randomUUID(), "craft0", project, projectCraftList, 0, userCsm)
    projectCraftList.addProjectCraftToList(listOf(projectCraft))

    val taskList: MutableList<Task> = LinkedList()
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task0")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(STARTED)
                .withoutAssignee()
                .withCreatedBy(userCsm)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(userCsm)
                .withLocation("TaskLocation0")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task1")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(fm)
                .withCreatedBy(userCsm)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(userCsm)
                .withLocation("TaskLocation1")
                .build()))
    taskList.add(
        taskRepository.save(
            task()
                .withName("Task2")
                .withIdentifier(TaskId())
                .withProject(project)
                .withStatus(OPEN)
                .withAssignee(fm)
                .withCreatedBy(userCsm)
                .withProjectCraft(projectCraft)
                .withLastModifiedBy(userCsm)
                .withLocation("TaskLocation2")
                .build()))

    // Create the tasks schedule
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[0])
            .withStart(LocalDate.of(2016, 3, 1))
            .withEnd(LocalDate.of(2016, 4, 30))
            .withCreatedBy(userCsm)
            .withLastModifiedBy(userCsm)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[1])
            .withStart(LocalDate.of(2016, 5, 1))
            .withEnd(null)
            .withCreatedBy(userCsm)
            .withLastModifiedBy(userCsm)
            .build())
    taskScheduleRepository.save(
        taskSchedule()
            .withTask(taskList[2])
            .withStart(null)
            .withEnd(LocalDate.of(2016, 6, 30))
            .withCreatedBy(userCsm)
            .withLastModifiedBy(userCsm)
            .build())

    // Setup the request
    LocaleContextHolder.setLocale(ENGLISH)
    val taskFilterDto =
        TaskFilterDto(
            projectRef = project.identifier,
            taskStatus = listOf(DRAFT, OPEN, STARTED, CLOSED),
            startAndEndDateMustBeSet = true)

    val pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order(ASC, COMPANY.property)))
    val tasks = taskRepository.findTaskIdentifiersForFilters(taskFilterDto, pageRequest)
    val count = taskRepository.countAllForFilters(taskFilterDto)

    assertThat(tasks).isNotNull
    assertThat(count).isEqualTo(1)
    assertThat(tasks).containsExactly(taskList[0].identifier)
  }

  // Method to create user with user id
  private fun createUser(userId: String, firstName: String, lastName: String): User =
      userRepository
          .save(
              user()
                  .withUserId(userId)
                  .withIdentifier(randomUUID())
                  .withFirstName(firstName)
                  .withLastName(lastName)
                  .build())
          .also {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(it, "n/a", emptyList())
          }

  // Method to create company with company id, name and create user
  private fun createCompany(uuid: UUID, companyName: String, user: User): Company =
      companyRepository.save(
          company()
              .withIdentifier(uuid)
              .withName(companyName)
              .withCreatedBy(user)
              .withLastModifiedBy(user)
              .build())

  // Method to create employee with company and role
  private fun createEmployee(
      company: Company,
      employeeRoleEnum: EmployeeRoleEnum,
      user: User
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

  // Method to create project with project id and create user
  private fun createProject(projectRef: ProjectId, user: UserId): Project =
      projectRepository.save(
          project().withCreatedBy(user).withLastModifiedBy(user).withIdentifier(projectRef).build())

  // Method to create participant with project, employee, role and create user
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

  // Method to create project craft with project craft id, project, project craft list,
  // position and user
  private fun createProjectCraft(
      uuid: UUID,
      name: String,
      project: Project,
      projectCraftList: ProjectCraftList,
      position: Int,
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
              .withProjectCraftList(projectCraftList)
              .withPosition(position)
              .build())

  // Method to create project craft list with project and user
  private fun createProjectCraftList(project: Project, user: User): ProjectCraftList =
      projectCraftListRepository.save(
          projectCraftList()
              .withProject(project)
              .withLastModifiedBy(user.getAuditUserId())
              .withCreatedBy(user.getAuditUserId())
              .build())

  // Method to add project craft to project craft list
  private fun ProjectCraftList.addProjectCraftToList(projectCrafts: List<ProjectCraft>) {
    projectCrafts.forEach { craft -> this.projectCrafts.add(craft.position ?: 0, craft) }
    projectCraftListRepository.save(this)
  }

  // Method to build work area with work area id, project, position and user
  private fun buildWorkArea(
      workAreaId: WorkAreaId,
      name: String,
      position: Int,
      project: Project,
      user: User
  ): WorkArea =
      workAreaRepository.save(
          workArea()
              .withProject(project)
              .withIdentifier(workAreaId)
              .withName(name)
              .withPosition(position)
              .withLastModifiedBy(user)
              .withCreatedBy(user)
              .build())

  // Method to build work area list with a project and user
  private fun buildWorkAreaList(project: Project, user: User): WorkAreaList =
      workAreaList()
          .withProject(project)
          .withWorkAreas(ArrayList())
          .withLastModifiedBy(user)
          .withCreatedBy(user)
          .build()

  // Method to build topic with criticality
  private fun buildTopic(topicCriticalityEnum: TopicCriticalityEnum, user: User): Topic =
      topic()
          .withIdentifier(randomUUID())
          .withCriticality(topicCriticalityEnum)
          .withLastModifiedBy(user)
          .withCreatedBy(user)
          .build()
}
