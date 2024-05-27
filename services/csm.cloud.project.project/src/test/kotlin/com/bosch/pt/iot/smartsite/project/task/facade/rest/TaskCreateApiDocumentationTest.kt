/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.SaveTaskResourceBuilder
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.CreateTaskBatchResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskCreateApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  private val participantIdentifier by lazy { getIdentifier("fm-participant").asParticipantId() }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  private val task: Task by lazy { repositories.taskRepository.findAll().first() }

  private val csmUser: User by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("csm-user"))!!
  }

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator.submitWorkArea().submitWorkAreaList()

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create single task with assignee and status open`() {
    val saveTaskResource =
        SaveTaskResourceBuilder()
            .setName("What a task")
            .setDescription("Task description")
            .setLocation("location")
            .setProjectCraftId(getIdentifier("projectCraft").asProjectCraftId())
            .setWorkAreaId(getIdentifier("workArea").asWorkAreaId())
            .setProjectId(projectIdentifier)
            .setAssigneeId(participantIdentifier)
            .setStatus(OPEN)
            .createSaveTaskResource()

    mockMvc
        .perform(requestBuilder(post(latestVersionOf(TASKS_ENDPOINT)), saveTaskResource))
        .andExpectAll(
            status().isCreated,
            header().string(ApiDocumentationSnippets.ETAG_HEADER, "\"2\""),
            header()
                .string(
                    LOCATION, matchesRegex(".*${latestVersionOf(TASKS_ENDPOINT)}/[-a-z\\d]{36}$")),
            *hasIdentifierAndVersion(task.identifier.toUuid(), 2),
            *isCreatedBy(csmUser),
            *isLastModifiedBy(csmUser),
        )
        .andDo(
            document(
                "tasks/document-create-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(SAVE_TASK_REQUEST_FIELDS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS),
                TASK_LINKS,
            ))

    verifyEventsCreatedAssignedSent(saveTaskResource)
  }

  @Test
  fun `verify and document create single task with assignee and status open and identifier`() {
    val taskIdentifier = randomUUID()

    val saveTaskResource =
        SaveTaskResourceBuilder()
            .setName("What a task")
            .setDescription("Task description")
            .setLocation("location")
            .setProjectCraftId(getIdentifier("projectCraft").asProjectCraftId())
            .setWorkAreaId(getIdentifier("workArea").asWorkAreaId())
            .setProjectId(projectIdentifier)
            .setAssigneeId(participantIdentifier)
            .setStatus(OPEN)
            .createSaveTaskResource()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(TASK_BY_TASK_ID_ENDPOINT), taskIdentifier), saveTaskResource))
        .andExpectAll(
            status().isCreated,
            header().string(ApiDocumentationSnippets.ETAG_HEADER, "\"2\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(".*${latestVersionOf(TASKS_ENDPOINT)}/$taskIdentifier$")),
            *hasIdentifierAndVersion(task.identifier.toUuid(), 2),
            *isCreatedBy(csmUser),
            *isLastModifiedBy(csmUser))
        .andDo(
            document(
                "tasks/document-create-task-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                requestFields(SAVE_TASK_REQUEST_FIELDS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                TASK_LINKS,
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS),
            ))

    verifyEventsCreatedAssignedSent(saveTaskResource)
  }

  @Test
  fun `verify and document create multiple tasks`() {
    val createTaskBatchResources =
        List(2) { index ->
          CreateTaskBatchResource(
              randomUUID(),
              "Task $index",
              "Task description $index",
              "location",
              OPEN,
              projectIdentifier,
              getIdentifier("projectCraft").asProjectCraftId(),
              participantIdentifier,
              getIdentifier("workArea").asWorkAreaId())
        }

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(TASKS_BATCH_ENDPOINT)),
                UpdateBatchRequestResource(createTaskBatchResources)))
        .andExpectAll(
            status().isOk,
        )
        .andDo(
            document(
                "tasks/document-create-multiple-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                CREATE_BATCH_RESOURCE_REQUEST_FIELDS_SNIPPET,
                TASKS_RESPONSE_FIELDS))

    verifyEventsCreatedAssignedSent(createTaskBatchResources)
  }

  private fun verifyEventsCreatedAssignedSent(resource: SaveTaskResource) {
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(TaskEventAvro::class.java, TaskEventAvro::class.java, TaskEventAvro::class.java))

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 1, false).also {
      verifyCreatedAggregate(it[0].aggregate, resource)
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, ASSIGNED, 1, false)
        .first()
        .aggregate
        .also {
          assertThat(it.getAssigneeIdentifier()?.asParticipantId()).isEqualTo(resource.assigneeId)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, SENT, 1, false)
        .first()
        .aggregate
        .also { assertThat(it.status.name).isEqualTo(resource.status.name) }
  }

  private fun verifyEventsCreatedAssignedSent(resources: List<CreateTaskBatchResource>) {

    projectEventStoreUtils.verifyContainsInSequence(
        // from batch operation
        BatchOperationStartedEventAvro::class.java,

        // from batch create
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,

        // from batch assign
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,

        // from batch sent
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java,
    )

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, resources.size, false)
        .also { events ->
          resources.indices.forEach { i ->
            verifyCreatedAggregate(events[i].aggregate, resources[i])
          }
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, ASSIGNED, resources.size, false)
        .also { events ->
          resources.indices.forEach { i ->
            val aggregate = events[i].aggregate
            val resource = resources[i]
            assertThat(aggregate.getAssigneeIdentifier()?.asParticipantId())
                .isEqualTo(resource.assigneeId)
          }
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, SENT, resources.size, false)
        .also { events ->
          resources.indices.forEach { i ->
            val aggregate = events[i].aggregate
            val resource = resources[i]
            assertThat(aggregate.status.name).isEqualTo(resource.status.name)
          }
        }
  }

  private fun verifyCreatedAggregate(aggregate: TaskAggregateAvro, resource: SaveTaskResource) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(this, TASK, csmUser)
        assertThat(getCraftIdentifier().asProjectCraftId()).isEqualTo(resource.projectCraftId)
        assertThat(description).isEqualTo(resource.description)
        assertThat(name).isEqualTo(resource.name)
        assertThat(getProjectIdentifier().asProjectId()).isEqualTo(resource.projectId)
        assertThat(location).isEqualTo(resource.location)
        assertThat(status.name).isEqualTo(TaskStatusEnumAvro.DRAFT.name)
        assertThat(getAssigneeIdentifier()).isNull()
        assertThat(getWorkAreaIdentifier()?.asWorkAreaId()).isEqualTo(resource.workAreaId)
      }
}
