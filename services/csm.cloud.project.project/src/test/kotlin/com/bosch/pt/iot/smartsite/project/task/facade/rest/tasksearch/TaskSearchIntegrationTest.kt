/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource.FilterAssigneeResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch.TaskSearchIntegrationTest.TaskScheduleTestCaseBuilder.Companion.closedSchedule
import com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch.TaskSearchIntegrationTest.TaskScheduleTestCaseBuilder.Companion.leftOpenSchedule
import com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch.TaskSearchIntegrationTest.TaskScheduleTestCaseBuilder.Companion.rightOpenSchedule
import com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch.TaskSearchIntegrationTest.TaskScheduleTestCaseBuilder.RangeEnum
import com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch.TaskSearchIntegrationTest.TaskScheduleTestCaseBuilder.RangeEnum.FROM
import com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch.TaskSearchIntegrationTest.TaskScheduleTestCaseBuilder.RangeEnum.TO
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.LinkedList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus.OK

@EnableAllKafkaListeners
class TaskSearchIntegrationTest : AbstractIntegrationTestV2() {

  companion object {
    private const val USER_CSM_REF = "userCsm2"
    private const val USER_FM_REF = "user"
    private const val PARTICIPANT_CSM_REF = "participantCsm2Project2"
    private const val PARTICIPANT_FM_REF = "participantFmProject2"
    private const val PROJECT_REF = "project2"
  }

  @Autowired private lateinit var taskSearchController: TaskSearchController

  private val project by lazy {
    repositories.findProject(getIdentifier(PROJECT_REF).asProjectId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProject(PROJECT_REF)
        .submitParticipantG3(PARTICIPANT_CSM_REF) {
          it.user = getByReference(USER_CSM_REF)
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(PARTICIPANT_FM_REF) {
          it.user = getByReference(USER_FM_REF)
          it.role = ParticipantRoleEnumAvro.FM
        }

    setAuthentication(USER_CSM_REF)
  }

  @AfterEach
  fun verifyNoEventsForTaskSearch() {
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find tasks with empty result`() {
    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks).isEmpty()
  }

  @Test
  fun `verify find tasks ordered by name`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "C" }
        .submitTask(randomString()) { it.name = "B" }
        .submitTask(randomString()) { it.name = "A" }
        .submitTask(randomString()) { it.name = "D" }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("A", "B", "C", "D")
  }

  @Test
  fun `verify find tasks ordered by topic`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "One critical one uncritical Topic" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.CRITICAL }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }
        .submitTask(randomString()) { it.name = "Without Topics" }
        .submitTask(randomString()) { it.name = "Three uncritical Topics" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("topic")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly(
            "Without Topics", "Three uncritical Topics", "One critical one uncritical Topic")
  }

  @Test
  fun `verify find tasks ordered by task status`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask(randomString()) { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTask(randomString()) {
          it.status = TaskStatusEnumAvro.CLOSED
          it.assignee = getByReference(PARTICIPANT_CSM_REF)
        }
        .submitTask(randomString()) { it.status = TaskStatusEnumAvro.STARTED }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("status")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<TaskStatusEnum> { it.status }
        .containsExactly(DRAFT, OPEN, STARTED, CLOSED)
  }

  @Test
  fun `verify find tasks ordered by location`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.location = "A" }
        .submitTask(randomString()) { it.location = "C" }
        .submitTask(randomString()) { it.location = "B" }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("location")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.location }
        .containsExactly("A", "B", "C")
  }

  @Test
  fun `verify find tasks ordered by start`() {
    val now = now()
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "Today" }
        .submitTaskSchedule(randomString()) { it.start = now.toEpochMilli() }
        .submitTask(randomString()) { it.name = "Earlier" }
        .submitTaskSchedule(randomString()) { it.start = now.minusWeeks(1).toEpochMilli() }
        .submitTask(randomString()) { it.name = "Later" }
        .submitTaskSchedule(randomString()) { it.start = now.plusWeeks(1).toEpochMilli() }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("start")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Earlier", "Today", "Later")
  }

  @Test
  fun `verify find tasks ordered by end`() {
    val now = now()
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "Today" }
        .submitTaskSchedule(randomString()) { it.end = now.toEpochMilli() }
        .submitTask(randomString()) { it.name = "Earlier" }
        .submitTaskSchedule(randomString()) { it.end = now.minusWeeks(1).toEpochMilli() }
        .submitTask(randomString()) { it.name = "Later" }
        .submitTaskSchedule(randomString()) { it.end = now.plusWeeks(1).toEpochMilli() }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("end")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Earlier", "Today", "Later")
  }

  @Test
  fun `verify find tasks ordered by company`() {
    val userCompanyBRef = "userCompanyB"
    val participantCompanyBRef = "participantCompanyB"
    val participant1CompanyARef = "participant1CompanyA"
    val participant2CompanyARef = "participant2CompanyA"
    val projectRef = "project3"

    eventStreamGenerator
        .setUserContext("system")
        .submitProject(projectRef)
        .submitCompany(randomString()) { it.name = "Company B" }
        .submitUser(userCompanyBRef)
        .submitEmployee(randomString())
        .submitParticipantG3(participantCompanyBRef) { it.role = ParticipantRoleEnumAvro.FM }
        .submitCompany(randomString()) { it.name = "Company A" }
        .submitUser(randomString())
        .submitEmployee(randomString())
        .submitParticipantG3(participant1CompanyARef) { it.role = ParticipantRoleEnumAvro.FM }
        .submitUser(randomString())
        .submitEmployee(randomString())
        .submitParticipantG3(participant2CompanyARef) { it.role = ParticipantRoleEnumAvro.FM }
        .submitTask(randomString()) { it.assignee = getByReference(participant2CompanyARef) }
        .submitTask(randomString()) { it.assignee = getByReference(participantCompanyBRef) }
        .submitTask(randomString()) { it.assignee = getByReference(participant1CompanyARef) }

    setAuthentication(userCompanyBRef)

    val response =
        taskSearchController.search(
            projectId = getIdentifier(projectRef).asProjectId(),
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("company")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.company?.displayName }
        .containsExactly("Company A", "Company A", "Company B")
  }

  @Test
  fun `verify find tasks ordered by project craft`() {
    eventStreamGenerator
        .submitProjectCraftG2("craftA") { it.name = "Craft A" }
        .submitProjectCraftG2("craftB") { it.name = "Craft B" }
        .submitProjectCraftG2("craftC") { it.name = "Craft C" }
        .submitProjectCraftList(eventType = ITEMADDED) {
          it.projectCrafts =
              listOf(getByReference("craftC"), getByReference("craftA"), getByReference("craftB"))
        }
        .submitTask(randomString()) {
          it.name = "Task Craft B"
          it.craft = getByReference("craftB")
        }
        .submitTask(randomString()) {
          it.name = "Task Craft C"
          it.craft = getByReference("craftC")
        }
        .submitTask(randomString()) {
          it.name = "Task Craft A"
          it.craft = getByReference("craftA")
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("projectCraft")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task Craft C", "Task Craft A", "Task Craft B")
  }

  @Test
  fun `verify find tasks filtered by topic criticality`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "Some critical task" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.CRITICAL }
        .submitTask(randomString()) { it.name = "Another critical task" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.CRITICAL }
        .submitTask(randomString()) { it.name = "An uncritical task" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }
        .submitTask(randomString()) { it.name = "Without topics" }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            FilterTaskListResource(topicCriticality = listOf(CRITICAL)),
            PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Another critical task", "Some critical task")
  }

  @Test
  fun `verify find tasks filtered by all days in date range`() {
    val now = now()
    val from = now.minusDays(3)
    val to = now.plusDays(3)

    eventStreamGenerator
        // |---------|
        .submitTask(randomString()) { it.name = "Task 1" }
        .submitTaskSchedule(randomString()) {
          it.start = from.toEpochMilli()
          it.end = to.toEpochMilli()
        }
        // |  -------|
        .submitTask(randomString()) { it.name = "Task 2" }
        .submitTaskSchedule(randomString()) {
          it.start = from.plusDays(1).toEpochMilli()
          it.end = to.toEpochMilli()
        }
        // |-------  |
        .submitTask(randomString()) { it.name = "Task 3" }
        .submitTaskSchedule(randomString()) {
          it.start = from.toEpochMilli()
          it.end = to.minusDays(1).toEpochMilli()
        }
        // |  -----  |
        .submitTask(randomString()) { it.name = "Task 4" }
        .submitTaskSchedule(randomString()) {
          it.start = from.plusDays(1).toEpochMilli()
          it.end = to.minusDays(1).toEpochMilli()
        }
        // |  -------|--
        .submitTask(randomString()) { it.name = "Task 5" }
        .submitTaskSchedule(randomString()) {
          it.start = from.plusDays(1).toEpochMilli()
          it.end = to.plusDays(1).toEpochMilli()
        }
        // --|-------  |
        .submitTask(randomString()) { it.name = "Task 6" }
        .submitTaskSchedule(randomString()) {
          it.start = from.minusDays(1).toEpochMilli()
          it.end = to.minusDays(1).toEpochMilli()
        }
        // |          | ----------
        .submitTask(randomString()) { it.name = "Task 7" }
        .submitTaskSchedule(randomString()) {
          it.start = from.plusDays(4).toEpochMilli()
          it.end = to.plusDays(5).toEpochMilli()
        }
        // ---------- |          |
        .submitTask(randomString()) { it.name = "Task 8" }
        .submitTaskSchedule(randomString()) {
          it.start = from.minusDays(5).toEpochMilli()
          it.end = to.minusDays(7).toEpochMilli()
        }
        // --|--------|--
        .submitTask(randomString()) { it.name = "Task 9" }
        .submitTaskSchedule(randomString()) {
          it.start = from.minusDays(1).toEpochMilli()
          it.end = to.plusDays(1).toEpochMilli()
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(from = from, to = to, allDaysInDateRange = true),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task 1", "Task 2", "Task 3", "Task 4")
  }

  @Test
  fun `verify find tasks filtered by all days in date range when rangeStartDate and rangeEndDate are null`() {
    val now = now()

    eventStreamGenerator
        .submitTask(randomString()) { it.name = "Task 1" }
        .submitTaskSchedule(randomString()) {
          it.start = now.minusDays(3).toEpochMilli()
          it.end = now.plusDays(3).toEpochMilli()
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(from = null, to = null, allDaysInDateRange = true),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks).extracting<String> { it.name }.containsExactly("Task 1")
  }

  @Test
  fun `verify find tasks filtered by all days in date range when rangeStartDate is null`() {
    val now = now()
    val to = now.plusDays(3)

    eventStreamGenerator
        // ---------|
        .submitTask(randomString()) { it.name = "Task 1" }
        .submitTaskSchedule(randomString()) {
          it.start = now.toEpochMilli()
          it.end = to.toEpochMilli()
        }
        // -------  |
        .submitTask(randomString()) { it.name = "Task 2" }
        .submitTaskSchedule(randomString()) {
          it.start = now.toEpochMilli()
          it.end = to.minusDays(1).toEpochMilli()
        }
        // -------|--
        .submitTask(randomString()) { it.name = "Task 3" }
        .submitTaskSchedule(randomString()) {
          it.start = now.toEpochMilli()
          it.end = to.plusDays(1).toEpochMilli()
        }
        //            | ----------
        .submitTask(randomString()) { it.name = "Task 4" }
        .submitTaskSchedule(randomString()) {
          it.start = to.plusDays(2).toEpochMilli()
          it.end = to.plusDays(5).toEpochMilli()
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(from = null, to = to, allDaysInDateRange = true),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task 1", "Task 2")
  }

  @Test
  fun `verify find tasks filtered by all days in date range when rangeEndDate is null`() {
    val now = now()
    val from = now.minusDays(3)

    eventStreamGenerator
        // |---------
        .submitTask(randomString()) { it.name = "Task 1" }
        .submitTaskSchedule(randomString()) {
          it.start = from.toEpochMilli()
          it.end = now.toEpochMilli()
        }
        // |  -------
        .submitTask(randomString()) { it.name = "Task 2" }
        .submitTaskSchedule(randomString()) {
          it.start = from.plusDays(1).toEpochMilli()
          it.end = now.toEpochMilli()
        }
        // --|-------
        .submitTask(randomString()) { it.name = "Task 3" }
        .submitTaskSchedule(randomString()) {
          it.start = from.minusDays(1).toEpochMilli()
          it.end = now.toEpochDay()
        }
        // ---------- |          |
        .submitTask(randomString()) { it.name = "Task 4" }
        .submitTaskSchedule(randomString()) {
          it.start = from.minusDays(5).toEpochMilli()
          it.end = from.minusDays(1).toEpochMilli()
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(from = from, to = null, allDaysInDateRange = true),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task 1", "Task 2")
  }

  @Test
  fun `verify find tasks filtered by has topics`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "Some task with topics" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.CRITICAL }
        .submitTask(randomString()) { it.name = "Another task with topics" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }
        .submitTask(randomString()) { it.name = "Without topics" }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(hasTopics = true),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Another task with topics", "Some task with topics")
  }

  @Test
  fun `verify find tasks filtered by has no topics`() {
    eventStreamGenerator
        .submitTask(randomString()) { it.name = "Some task with topics" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.CRITICAL }
        .submitTask(randomString()) { it.name = "Another task with topics" }
        .submitTopicG2(randomString()) { it.criticality = TopicCriticalityEnumAvro.UNCRITICAL }
        .submitTask(randomString()) { it.name = "Without topics" }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(hasTopics = false),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Without topics")
  }

  @Test
  fun `verify find tasks filtered by task status`() {
    eventStreamGenerator
        .submitTask(randomString()) {
          it.name = "Draft"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(randomString()) {
          it.name = "Open"
          it.status = TaskStatusEnumAvro.OPEN
        }
        .submitTask(randomString()) {
          it.name = "Started"
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask(randomString()) {
          it.name = "Closed"
          it.status = TaskStatusEnumAvro.CLOSED
          it.assignee = getByReference(PARTICIPANT_CSM_REF)
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = FilterTaskListResource(status = listOf(OPEN, STARTED, CLOSED)),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Closed", "Open", "Started")
  }

  @Test
  fun `verify find tasks filtered by project crafts`() {
    eventStreamGenerator
        .submitProjectCraftG2("craftA") { it.name = "Craft A" }
        .submitProjectCraftG2("craftB") { it.name = "Craft B" }
        .submitTask(randomString()) {
          it.status = TaskStatusEnumAvro.DRAFT
          it.craft = getByReference("craftB")
        }
        .submitTask(randomString()) {
          it.status = TaskStatusEnumAvro.DRAFT
          it.craft = getByReference("craftA")
        }

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter =
                FilterTaskListResource(
                    projectCraftIds = listOf(getIdentifier("craftA").asProjectCraftId())),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.projectCraft.name }
        .containsExactly("Craft A")
  }

  @Nested
  inner class FindTasksFilteredByWorkAreas {

    private val workArea1 by lazy {
      repositories.findWorkArea(getIdentifier("workArea1").asWorkAreaId())!!
    }
    private val taskWorkArea1 by lazy {
      repositories.findTaskWithDetails(getIdentifier("taskWorkArea1").asTaskId())!!
    }
    private val taskWithoutWorkArea by lazy {
      repositories.findTaskWithDetails(getIdentifier("taskWithoutWorkArea").asTaskId())!!
    }

    @BeforeEach
    fun beforeEach() {
      eventStreamGenerator
          .submitWorkArea("workArea1") { it.name = "Work Area 1" }
          .submitWorkArea("workArea2") { it.name = "Work Area 2" }
          .submitWorkAreaList { it.workAreas = listOf(getByReference("workArea1")) }
          .submitTask("taskWorkArea1") { it.workarea = getByReference("workArea1") }
          .submitTask("taskWorkArea2") { it.workarea = getByReference("workArea2") }
          .submitTask("taskWithoutWorkArea")
    }

    @Test
    fun `verify find tasks filtered by work areas and project`() {
      val response =
          taskSearchController.search(
              project.identifier,
              FilterTaskListResource(workAreaIds = listOf(WorkAreaIdOrEmpty(workArea1.identifier))),
              PageRequest.of(0, 10, Sort.by("name")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.tasks).hasSize(1)
      assertThat(response.body!!.tasks)
          .extracting<String> { it.name }
          .containsExactlyInAnyOrder(taskWorkArea1.name)
    }

    @Test
    fun `verify find tasks filtered by work areas or empty work area`() {
      val response =
          taskSearchController.search(
              projectId = project.identifier,
              filter =
                  FilterTaskListResource(
                      workAreaIds =
                          listOf(WorkAreaIdOrEmpty(workArea1.identifier), WorkAreaIdOrEmpty())),
              pageable = PageRequest.of(0, 10, Sort.by("name")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.tasks).hasSize(2)
      assertThat(response.body!!.tasks)
          .extracting<String> { it.name }
          .containsExactlyInAnyOrder(taskWorkArea1.name, taskWithoutWorkArea.name)
    }

    @Test
    fun `verify find tasks filtered by empty work area`() {
      val response =
          taskSearchController.search(
              projectId = project.identifier,
              filter = FilterTaskListResource(workAreaIds = listOf(WorkAreaIdOrEmpty())),
              pageable = PageRequest.of(0, 10, Sort.by("name")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.tasks).hasSize(1)
      assertThat(response.body!!.tasks)
          .extracting<String> { it.name }
          .containsExactly(taskWithoutWorkArea.name)
    }
  }

  @Test
  fun `verify find tasks filtered by projects`() {
    val project1Ref = randomString()
    val participant1Ref = randomString()

    eventStreamGenerator
        .submitProject(project1Ref)
        .submitParticipantG3(participant1Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitTask(randomString()) { it.name = "Task Project 1" }
        .submitProject(randomString())
        .submitParticipantG3(randomString()) { it.role = ParticipantRoleEnumAvro.FM }
        .submitTask(randomString()) { it.name = "Task Project 2" }

    val participant1 =
        repositories.participantRepository.findOneWithDetailsByIdentifier(
            getIdentifier(participant1Ref).asParticipantId())!!
    setAuthentication(participant1.user!!.identifier!!)

    val response =
        taskSearchController.search(
            projectId = getIdentifier(project1Ref).asProjectId(),
            filter = FilterTaskListResource(),
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task Project 1")
  }

  @Test
  fun `verify find tasks filtered by assigned company`() {
    val company1Ref = randomString()
    val user1Ref = randomString()
    val participant1Ref = randomString()
    val participant2Ref = randomString()

    eventStreamGenerator
        .submitCompany(company1Ref)
        .submitUser(user1Ref)
        .submitEmployee(randomString())
        .submitParticipantG3(participant1Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitCompany(randomString())
        .submitUser(randomString())
        .submitEmployee(randomString())
        .submitParticipantG3(participant2Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitTask(randomString()) {
          it.name = "Task Company 1"
          it.assignee = getByReference(participant1Ref)
        }
        .submitTask(randomString()) {
          it.name = "Task Company 2"
          it.assignee = getByReference(participant2Ref)
        }

    val filterResource =
        FilterTaskListResource(
            assignees = FilterAssigneeResource(companyIds = listOf(getIdentifier(company1Ref))))

    setAuthentication(user1Ref)

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = filterResource,
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task Company 1")
  }

  @Test
  fun `verify find tasks filtered by assigned participant`() {
    val company1Ref = randomString()
    val user1Ref = randomString()
    val participant1Ref = randomString()
    val participant2Ref = randomString()

    eventStreamGenerator
        .submitCompany(company1Ref)
        .submitUser(user1Ref)
        .submitEmployee(randomString())
        .submitParticipantG3(participant1Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitCompany(randomString())
        .submitUser(randomString())
        .submitEmployee(randomString())
        .submitParticipantG3(participant2Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitTask(randomString()) {
          it.name = "Task Company 1"
          it.assignee = getByReference(participant1Ref)
        }
        .submitTask(randomString()) {
          it.name = "Task Company 2"
          it.assignee = getByReference(participant2Ref)
        }

    val filterResource =
        FilterTaskListResource(
            assignees =
                FilterAssigneeResource(
                    participantIds = listOf(getIdentifier(participant2Ref).asParticipantId())))

    setAuthentication(user1Ref)

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = filterResource,
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task Company 2")
  }

  @Test
  fun `verify find tasks filtered by assigned participant or company`() {
    val company2Ref = randomString()
    val user1Ref = randomString()
    val participant1Ref = randomString()
    val participant2Ref = randomString()
    val participant3Ref = randomString()

    eventStreamGenerator
        .submitCompany(randomString())
        .submitUser(user1Ref)
        .submitEmployee(randomString())
        .submitParticipantG3(participant1Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitCompany(company2Ref)
        .submitUser(randomString())
        .submitEmployee(randomString())
        .submitParticipantG3(participant2Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitCompany(randomString())
        .submitUser(randomString())
        .submitEmployee(randomString())
        .submitParticipantG3(participant3Ref) { it.role = ParticipantRoleEnumAvro.FM }
        .submitTask(randomString()) {
          it.name = "Task Company 1"
          it.assignee = getByReference(participant1Ref)
        }
        .submitTask(randomString()) {
          it.name = "Task Company 2"
          it.assignee = getByReference(participant2Ref)
        }
        .submitTask(randomString()) {
          it.name = "Task Company 3"
          it.assignee = getByReference(participant3Ref)
        }

    val filterResource =
        FilterTaskListResource(
            assignees =
                FilterAssigneeResource(
                    participantIds = listOf(getIdentifier(participant3Ref).asParticipantId()),
                    companyIds = listOf(getIdentifier(company2Ref))))

    setAuthentication(user1Ref)

    val response =
        taskSearchController.search(
            projectId = project.identifier,
            filter = filterResource,
            pageable = PageRequest.of(0, 10, Sort.by("name")))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsExactly("Task Company 2", "Task Company 3")
  }

  @Test
  fun `verify find tasks filtered by start date for closed schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            closedSchedule().startingBefore(FROM).endingBefore(FROM).assertNotContained(),
            closedSchedule().startingBefore(FROM).endingAt(FROM).assertContained(),
            closedSchedule().startingBefore(FROM).endingAfter(FROM).assertContained(),
            closedSchedule().startingAt(FROM).endingAt(FROM).assertContained(),
            closedSchedule().startingAt(FROM).endingAfter(FROM).assertContained(),
            closedSchedule().startingAfter(FROM).endingAfter(FROM).assertContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val fromDate = testCases.first().from()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(from = fromDate))
  }

  @Test
  fun `verify find tasks filtered by start date for left open schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            leftOpenSchedule().endingBefore(FROM).assertNotContained(),
            leftOpenSchedule().endingAt(FROM).assertContained(),
            leftOpenSchedule().endingAfter(FROM).assertContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val fromDate = testCases.first().from()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(from = fromDate))
  }

  @Test
  fun `verify find tasks filtered by start date for right open schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            rightOpenSchedule().startingBefore_(FROM).assertContained(),
            rightOpenSchedule().startingAt_(FROM).assertContained(),
            rightOpenSchedule().startingAfter_(FROM).assertContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val fromDate = testCases.first().from()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(from = fromDate))
  }

  @Test
  fun `verify find tasks filtered by end date for closed schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            closedSchedule().startingBefore(TO).endingBefore(TO).assertContained(),
            closedSchedule().startingBefore(TO).endingAt(TO).assertContained(),
            closedSchedule().startingBefore(TO).endingAfter(TO).assertContained(),
            closedSchedule().startingAt(TO).endingAt(TO).assertContained(),
            closedSchedule().startingAt(TO).endingAfter(TO).assertContained(),
            closedSchedule().startingAfter(TO).endingAfter(TO).assertNotContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val toDate = testCases.first().to()

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(to = toDate))
  }

  @Test
  fun `verify find tasks filtered by end date for left open schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            leftOpenSchedule().endingBefore(TO).assertContained(),
            leftOpenSchedule().endingAt(TO).assertContained(),
            leftOpenSchedule().endingAfter(TO).assertContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val toDate = testCases.first().to()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(to = toDate))
  }

  @Test
  fun `verify find tasks filtered by end date for right open schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            rightOpenSchedule().startingBefore_(TO).assertContained(),
            rightOpenSchedule().startingAt_(TO).assertContained(),
            rightOpenSchedule().startingAfter_(TO).assertNotContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val toDate = testCases.first().to()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(to = toDate))
  }

  @Test
  fun `verify find tasks filtered by start and end date for closed schedule`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            closedSchedule().startingBefore(FROM).endingBefore(FROM).assertNotContained(),
            closedSchedule().startingBefore(FROM).endingAt(FROM).assertContained(),
            closedSchedule().startingBefore(FROM).endingAfter(FROM).assertContained(),
            closedSchedule().startingAt(FROM).endingAt(FROM).assertContained(),
            closedSchedule().startingAt(FROM).endingAfter(FROM).assertContained(),
            closedSchedule().startingAfter(FROM).endingAfter(FROM).assertContained(), //
            closedSchedule().startingBefore(FROM).endingBefore(TO).assertContained(),
            closedSchedule().startingBefore(FROM).endingAt(TO).assertContained(),
            closedSchedule().startingBefore(FROM).endingAfter(TO).assertContained(),
            closedSchedule().startingAt(FROM).endingBefore(TO).assertContained(),
            closedSchedule().startingAt(FROM).endingAt(TO).assertContained(),
            closedSchedule().startingAt(FROM).endingAfter(TO).assertContained(),
            closedSchedule().startingAfter(FROM).endingBefore(TO).assertContained(),
            closedSchedule().startingAfter(FROM).endingAt(TO).assertContained(),
            closedSchedule().startingAfter(FROM).endingAfter(TO).assertContained(), //
            closedSchedule().startingBefore(TO).endingBefore(TO).assertContained(),
            closedSchedule().startingBefore(TO).endingAt(TO).assertContained(),
            closedSchedule().startingBefore(TO).endingAfter(TO).assertContained(),
            closedSchedule().startingAt(TO).endingAt(TO).assertContained(),
            closedSchedule().startingAt(TO).endingAfter(TO).assertContained(),
            closedSchedule().startingAfter(TO).endingAfter(TO).assertNotContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val fromDate = testCases.first().from()!!
    val toDate = testCases.first().to()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(from = fromDate, to = toDate))
  }

  @Test
  fun `verify find tasks filtered by start and end date for left open schedules`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            leftOpenSchedule().endingBefore(FROM).assertNotContained(),
            leftOpenSchedule().endingAt(FROM).assertContained(),
            leftOpenSchedule().endingAfter(FROM).assertContained(),
            leftOpenSchedule().endingBefore(TO).assertContained(),
            leftOpenSchedule().endingAt(TO).assertContained(),
            leftOpenSchedule().endingAfter(TO).assertContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val fromDate = testCases.first().from()!!
    val toDate = testCases.first().to()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(from = fromDate, to = toDate))
  }

  @Test
  fun `verify find tasks filtered by start and end date for right open schedules`() {
    val testCases: List<TaskScheduleTestCase> =
        listOf(
            rightOpenSchedule().startingBefore_(FROM).assertContained(),
            rightOpenSchedule().startingAt_(FROM).assertContained(),
            rightOpenSchedule().startingAfter_(FROM).assertContained(),
            rightOpenSchedule().startingBefore_(TO).assertContained(),
            rightOpenSchedule().startingAt_(TO).assertContained(),
            rightOpenSchedule().startingAfter_(TO).assertNotContained())

    testCases.forEach { createTaskWithSchedule(it) }

    val fromDate = testCases.first().from()!!
    val toDate = testCases.first().to()!!

    assertTestCases(
        projectIdentifier = project.identifier,
        taskScheduleTestCases = testCases,
        filterResource = FilterTaskListResource(from = fromDate, to = toDate))
  }

  private fun createTaskWithSchedule(taskScheduleTestCase: TaskScheduleTestCase) {
    eventStreamGenerator
        .submitTask(randomString()) {
          it.status = TaskStatusEnumAvro.DRAFT
          it.name = taskScheduleTestCase.name()
        }
        .submitTaskSchedule(randomString()) {
          it.start = taskScheduleTestCase.start()?.toEpochMilli()
          it.end = taskScheduleTestCase.end()?.toEpochMilli()
        }
  }

  private fun assertTestCases(
      projectIdentifier: ProjectId,
      taskScheduleTestCases: List<TaskScheduleTestCase>,
      filterResource: FilterTaskListResource
  ) {
    val response =
        taskSearchController.search(
            projectId = projectIdentifier,
            filter = filterResource,
            pageable = PageRequest.of(0, taskScheduleTestCases.size))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.tasks)
        .extracting<String> { it.name }
        .containsOnly(
            *taskScheduleTestCases.filter { it.isContained }.map { it.name() }.toTypedArray())
  }

  class TaskScheduleTestCaseBuilder private constructor() :
      StartDefinition,
      StartWithoutEndDefinition,
      EndDefinition,
      AssertionDefinition,
      TaskScheduleTestCase {

    var fromDate: LocalDate
    var toDate: LocalDate
    var scheduleStart: LocalDate? = null
    var scheduleEnd: LocalDate? = null
    var name: MutableList<String?> = LinkedList()

    override var isContained = false
      private set

    override fun startingBefore(rangeEnum: RangeEnum): EndDefinition {
      name.add("starts before")
      name.add(rangeEnum.value)
      scheduleStart = toDate(rangeEnum).minusDays(2)
      return this
    }

    override fun startingAt(rangeEnum: RangeEnum): TaskScheduleTestCaseBuilder {
      name.add("starts at")
      name.add(rangeEnum.value)
      scheduleStart = toDate(rangeEnum)
      return this
    }

    override fun startingAfter(rangeEnum: RangeEnum): TaskScheduleTestCaseBuilder {
      name.add("starts after")
      name.add(rangeEnum.value)
      scheduleStart = toDate(rangeEnum).plusDays(1)
      return this
    }

    override fun endingBefore(rangeEnum: RangeEnum): AssertionDefinition {
      name.add("ends before")
      name.add(rangeEnum.value)
      scheduleEnd = toDate(rangeEnum).minusDays(1)
      return this
    }

    override fun endingAt(rangeEnum: RangeEnum): AssertionDefinition {
      name.add("ends at")
      name.add(rangeEnum.value)
      scheduleEnd = toDate(rangeEnum)
      return this
    }

    override fun endingAfter(rangeEnum: RangeEnum): AssertionDefinition {
      name.add("ends after")
      name.add(rangeEnum.value)
      scheduleEnd = toDate(rangeEnum).plusDays(2)
      return this
    }

    private fun toDate(rangeEnum: RangeEnum): LocalDate =
        if (rangeEnum == FROM) fromDate else toDate

    override fun startingBefore_(rangeEnum: RangeEnum): AssertionDefinition {
      startingBefore(rangeEnum)
      return this
    }

    override fun startingAt_(rangeEnum: RangeEnum): AssertionDefinition {
      startingAt(rangeEnum)
      return this
    }

    override fun startingAfter_(rangeEnum: RangeEnum): AssertionDefinition {
      startingAfter(rangeEnum)
      return this
    }

    enum class RangeEnum(var value: String) {
      FROM("<from>"),
      TO("<to>")
    }

    override fun name(): String = name.joinToString(" ")

    override fun start(): LocalDate? = scheduleStart

    override fun end(): LocalDate? = scheduleEnd

    override fun from(): LocalDate = fromDate

    override fun to(): LocalDate = toDate

    override fun assertContained(): TaskScheduleTestCase {
      assertThat(
              scheduleStart == null || scheduleEnd == null || !scheduleStart!!.isAfter(scheduleEnd))
          .isTrue
      isContained = true
      return this
    }

    override fun assertNotContained(): TaskScheduleTestCase {
      assertThat(
              scheduleStart == null || scheduleEnd == null || !scheduleStart!!.isAfter(scheduleEnd))
          .isTrue
      isContained = false
      return this
    }

    companion object {

      /** a closed schedule is a schedule with both a start and an end date defined. */
      fun closedSchedule(): StartDefinition = TaskScheduleTestCaseBuilder()

      /** a left-open schedule is a schedule with an end date, but no start date defined. */
      fun leftOpenSchedule(): EndDefinition {
        val builder = TaskScheduleTestCaseBuilder()
        builder.scheduleStart = null
        return builder
      }

      /** a right-open schedule is a schedule with a start date, but no end date defined. */
      fun rightOpenSchedule(): StartWithoutEndDefinition {
        val builder = TaskScheduleTestCaseBuilder()
        builder.scheduleEnd = null
        return builder
      }
    }

    init {
      fromDate = now()
      toDate = fromDate.plusDays(10)
    }
  }

  interface StartDefinition {
    fun startingBefore(rangeEnum: RangeEnum): EndDefinition

    fun startingAt(rangeEnum: RangeEnum): EndDefinition

    fun startingAfter(rangeEnum: RangeEnum): EndDefinition
  }

  interface StartWithoutEndDefinition {
    fun startingBefore_(rangeEnum: RangeEnum): AssertionDefinition

    fun startingAt_(rangeEnum: RangeEnum): AssertionDefinition

    fun startingAfter_(rangeEnum: RangeEnum): AssertionDefinition
  }

  interface EndDefinition {
    fun endingBefore(rangeEnum: RangeEnum): AssertionDefinition

    fun endingAt(rangeEnum: RangeEnum): AssertionDefinition

    fun endingAfter(rangeEnum: RangeEnum): AssertionDefinition
  }

  interface AssertionDefinition {
    fun assertContained(): TaskScheduleTestCase

    fun assertNotContained(): TaskScheduleTestCase
  }

  interface TaskScheduleTestCase {
    fun name(): String?

    fun start(): LocalDate?

    fun end(): LocalDate?

    fun from(): LocalDate?

    fun to(): LocalDate?

    val isContained: Boolean
  }
}
