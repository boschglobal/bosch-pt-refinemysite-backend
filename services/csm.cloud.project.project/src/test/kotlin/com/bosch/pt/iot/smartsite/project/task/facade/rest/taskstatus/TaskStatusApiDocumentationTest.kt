/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskstatus

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_ACCEPTED_TASK_ACCEPT_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_CLOSE_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_DRAFT_OR_OPEN_TASK_RESET_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ACCEPT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CLOSE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CONSTRAINTS
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CONSTRAINTS_UPDATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_START
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_SCHEDULE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_UPDATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TOPIC
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TOPIC_CREATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_UNASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ACCEPT_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ACCEPT_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.CLOSE_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.CLOSE_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.RESET_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.RESET_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.SEND_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.SEND_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.START_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.START_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import java.util.Locale.ENGLISH
import java.util.Locale.GERMAN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskStatusApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  private val task1: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(getIdentifier("task-1").asTaskId())!!
  }

  private val task2: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(getIdentifier("task-2").asTaskId())!!
  }

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitWorkArea()
        .submitWorkAreaList()
        .submitTask("task-1") {
          it.status = DRAFT
          it.assignee = getByReference("fm-participant")
        }
        .submitTaskSchedule()
        .submitTask("task-2") {
          it.status = DRAFT
          it.assignee = getByReference("fm-participant")
        }
        .submitTaskSchedule()

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document send single task`() {

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(SEND_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").value(task1.identifier.toString()),
            jsonPath("$.status").value(OPEN.name))
        .andDo(
            document(
                "tasks/document-send-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                links(
                    linkWithRel(LINK_ASSIGN).description(LINK_ASSIGNED_FOREMAN_DESCRIPTION),
                    linkWithRel(LINK_UNASSIGN).description(LINK_UNASSIGNED_FOREMAN_DESCRIPTION),
                    linkWithRel(LINK_START).description(LINK_START_DESCRIPTION),
                    linkWithRel(LINK_CLOSE).description(LINK_CLOSE_DESCRIPTION),
                    linkWithRel(LINK_ACCEPT).description(LINK_ACCEPT_DESCRIPTION),
                    linkWithRel(LINK_TOPIC_CREATE).description(LINK_TOPIC_CREATE_DESCRIPTION),
                    linkWithRel(LINK_CONSTRAINTS_UPDATE)
                        .description(LINK_CONSTRAINTS_UPDATE_DESCRIPTION)
                        .optional(),
                    linkWithRel(LINK_CONSTRAINTS)
                        .description(LINK_CONSTRAINTS_DESCRIPTION)
                        .optional(),
                    linkWithRel(LINK_TOPIC).description(LINK_TOPIC_DESCRIPTION),
                    linkWithRel(LINK_DELETE).description(LINK_TASK_DELETE_DESCRIPTION),
                    linkWithRel(LINK_TASK_UPDATE).description(LINK_TASK_UPDATE_DESCRIPTION),
                    linkWithRel(LINK_TASK_SCHEDULE).description(LINK_TASK_SCHEDULE_DESCRIPTION),
                ),
                responseHeaders(headerWithName(ETAG_HEADER).description(ETAG_HEADER_DESCRIPTOR)),
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    val task1Updated = repositories.taskRepository.findOneByIdentifier(task1.identifier)!!

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, SENT, 1, true)
        .first()
        .aggregate
        .apply {
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(this, task1Updated, TASK)
          assertThat(status.name).isEqualTo(OPEN.name)
        }
  }

  @Test
  fun `verify and document send multiple tasks`() {
    val taskResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(SEND_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                taskResource))
        .andExpectAll(status().isOk, content().contentType(HAL_JSON_VALUE))
        .andDo(
            document(
                "tasks/document-batch-send-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, SENT, 2, false).also {
      it.first().aggregate.apply { assertThat(status).isEqualTo(TaskStatusEnumAvro.OPEN) }
      it.second().aggregate.apply { assertThat(status).isEqualTo(TaskStatusEnumAvro.OPEN) }
    }
  }

  @Test
  fun `verify and document start task`() {

    eventStreamGenerator.submitTask("task-1", eventType = SENT) {
      it.status = TaskStatusEnumAvro.OPEN
    }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(START_TASK_BY_TASK_ID_ENDPOINT), task1.identifier))
                .locale(GERMAN))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").value(task1.identifier.toString()),
            jsonPath("$.name").value(task1.name),
            jsonPath("$.status").value(STARTED.toString()),
            header().exists(ETAG_HEADER))
        .andDo(
            document(
                "tasks/document-start-task",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("The id of the task")),
                TASK_LINKS,
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    val task1Updated = repositories.taskRepository.findOneByIdentifier(task1.identifier)!!

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.STARTED, 1, true)
        .first()
        .aggregate
        .apply {
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(this, task1Updated, TASK)
          assertThat(status).isEqualTo(TaskStatusEnumAvro.STARTED)
        }
  }

  @Test
  fun `verify and document start tasks`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-2", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }

    projectEventStoreUtils.reset()

    val tasksResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(START_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                tasksResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(2))
        .andDo(
            document(
                "tasks/document-batch-start-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project that the task belongs to")),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.STARTED, 2, false)
        .also {
          assertThat(it[0].aggregate.status).isEqualTo(TaskStatusEnumAvro.STARTED)
          assertThat(it[1].aggregate.status).isEqualTo(TaskStatusEnumAvro.STARTED)
        }
  }

  @Test
  fun `verify start already started task fails`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(START_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isBadRequest,
            jsonPath("$.message")
                .value(
                    messageSource.getMessage(
                        TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN,
                        arrayOf(),
                        ENGLISH)))
        .andDo(
            document(
                "tasks/document-start-task-invalid-transition",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document close task`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(CLOSE_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isOk,
            header().exists(ETAG_HEADER),
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").value(task1.identifier.toString()),
            jsonPath("$.name").value(task1.name),
            jsonPath("$.status").value(CLOSED.toString()),
        )
        .andDo(
            document(
                "tasks/document-close-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                TASK_LINKS,
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    val task1Updated = repositories.taskRepository.findOneByIdentifier(task1.identifier)!!

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CLOSED, 1, true)
        .first()
        .aggregate
        .also { aggregate ->
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(aggregate, task1Updated, TASK)
          assertThat(aggregate.status).isEqualTo(TaskStatusEnumAvro.CLOSED)
        }
  }

  @Test
  fun `verify and document close tasks`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("task-2", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-2", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    val tasksResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(CLOSE_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                tasksResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(2))
        .andDo(
            document(
                "tasks/document-batch-close-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project that the task belongs to")),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CLOSED, 2, false)
        .also {
          assertThat(it[0].aggregate.status).isEqualTo(TaskStatusEnumAvro.CLOSED)
          assertThat(it[1].aggregate.status).isEqualTo(TaskStatusEnumAvro.CLOSED)
        }
  }

  @Test
  fun `verify close closed task fails`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("task-1", eventType = TaskEventEnumAvro.CLOSED) {
          it.status = TaskStatusEnumAvro.CLOSED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(CLOSE_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isBadRequest,
            jsonPath("$.message")
                .value(
                    messageSource.getMessage(
                        TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_CLOSE_FORBIDDEN,
                        arrayOf(),
                        ENGLISH)))
        .andDo(
            document(
                "tasks/document-close-task-invalid-transition",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document accept task`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(ACCEPT_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isOk,
            header().exists(ETAG_HEADER),
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").value(task1.identifier.toString()),
            jsonPath("$.name").value(task1.name),
            jsonPath("$.status").value(ACCEPTED.toString()),
        )
        .andDo(
            document(
                "tasks/document-accept-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                TASK_LINKS,
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    val task1Updated = repositories.taskRepository.findOneByIdentifier(task1.identifier)!!

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.ACCEPTED, 1, true)
        .first()
        .aggregate
        .also { aggregate ->
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(aggregate, task1Updated, TASK)
          assertThat(aggregate.status).isEqualTo(TaskStatusEnumAvro.ACCEPTED)
        }
  }

  @Test
  fun `verify and document accept tasks`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("task-2", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-2", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    val tasksResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(ACCEPT_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                tasksResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(2))
        .andDo(
            document(
                "tasks/document-batch-accept-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project that the task belongs to")),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.ACCEPTED, 2, false)
        .also {
          assertThat(it[0].aggregate.status).isEqualTo(TaskStatusEnumAvro.ACCEPTED)
          assertThat(it[1].aggregate.status).isEqualTo(TaskStatusEnumAvro.ACCEPTED)
        }
  }

  @Test
  fun `verify accept accept task fails`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("task-1", eventType = TaskEventEnumAvro.ACCEPTED) {
          it.status = TaskStatusEnumAvro.ACCEPTED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(ACCEPT_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isBadRequest,
            jsonPath("$.message")
                .value(
                    messageSource.getMessage(
                        TASK_VALIDATION_ERROR_ACCEPTED_TASK_ACCEPT_FORBIDDEN, arrayOf(), ENGLISH)))
        .andDo(
            document(
                "tasks/document-accept-task-invalid-transition",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document reset task`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(RESET_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isOk,
            header().exists(ETAG_HEADER),
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").value(task1.identifier.toString()),
            jsonPath("$.name").value(task1.name),
            jsonPath("$.status").value(OPEN.toString()),
        )
        .andDo(
            document(
                "tasks/document-reset-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                TASK_LINKS,
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    val task1Updated = repositories.taskRepository.findOneByIdentifier(task1.identifier)!!

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, RESET, 1, true)
        .first()
        .aggregate
        .also { aggregate ->
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(aggregate, task1Updated, TASK)
          assertThat(aggregate.status).isEqualTo(TaskStatusEnumAvro.OPEN)
        }
  }

  @Test
  fun `verify and document reset tasks`() {

    eventStreamGenerator
        .submitTask("task-1", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("task-2", eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }
        .submitTask("task-2", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }

    projectEventStoreUtils.reset()

    val tasksResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(RESET_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                tasksResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(2))
        .andDo(
            document(
                "tasks/document-batch-reset-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project that the task belongs to")),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, RESET, 2, false).also {
      assertThat(it[0].aggregate.status).isEqualTo(TaskStatusEnumAvro.OPEN)
      assertThat(it[1].aggregate.status).isEqualTo(TaskStatusEnumAvro.OPEN)
    }
  }

  @Test
  fun `verify reset task fails for already open task`() {

    eventStreamGenerator.submitTask("task-1", eventType = SENT) {
      it.status = TaskStatusEnumAvro.OPEN
    }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(RESET_TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isBadRequest,
            jsonPath("$.message")
                .value(
                    messageSource.getMessage(
                        TASK_VALIDATION_ERROR_DRAFT_OR_OPEN_TASK_RESET_FORBIDDEN,
                        arrayOf(),
                        ENGLISH)))
        .andDo(
            document(
                "tasks/document-reset-task-invalid-transition",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())))

    projectEventStoreUtils.verifyEmpty()
  }
}
