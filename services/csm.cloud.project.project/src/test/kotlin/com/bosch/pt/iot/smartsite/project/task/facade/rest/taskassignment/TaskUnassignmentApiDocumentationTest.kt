/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskassignment

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.errorMessageWithKey
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.UNASSIGN_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.UNASSIGN_TASK_BY_TASK_ID_ENDPOINT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskUnassignmentApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  private val task1Identifier by lazy { getIdentifier("task-1") }
  private val task2Identifier by lazy { getIdentifier("task-2") }

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitTask("task-1") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTask("task-2") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTask("task-1", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }
        .submitTask("task-2", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document remove assignee from single task`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(UNASSIGN_TASK_BY_TASK_ID_ENDPOINT), task1Identifier)))
        .andExpectAll(
            status().isOk,
            jsonPath("$.id").value(task1Identifier.toString()),
            jsonPath("$.assignee.displayName").doesNotExist(),
            jsonPath("$.assignee.id").doesNotExist())
        .andDo(
            document(
                "tasks/document-unassign-task-from-project-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                TASK_LINKS,
                responseHeaders(
                    headerWithName(ETAG_HEADER)
                        .description(
                            "Entity tag of the task, needed for possible updates of the task")),
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 1, true)
        .first()
        .aggregate
        .apply {
          assertThat(getIdentifier()).isEqualTo(task1Identifier)
          assertThat(getAssigneeIdentifier()).isNull()
        }
  }

  @Test
  fun `verify remove assignee from single task fails when task is closed`() {
    eventStreamGenerator
        .submitTask("task-1", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
        }
        .submitTask("task-1", eventType = TaskEventEnumAvro.STARTED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("task-1", eventType = TaskEventEnumAvro.CLOSED) {
          it.status = TaskStatusEnumAvro.CLOSED
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(UNASSIGN_TASK_BY_TASK_ID_ENDPOINT), task1Identifier)))
        .andExpectAll(
            status().isBadRequest,
            errorMessageWithKey(
                TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED,
                messageSource))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify remove assignee from single task fails when task is accepted`() {
    eventStreamGenerator
        .submitTask("task-1", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
        }
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
                post(latestVersionOf(UNASSIGN_TASK_BY_TASK_ID_ENDPOINT), task1Identifier)))
        .andExpectAll(
            status().isBadRequest,
            errorMessageWithKey(
                TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED,
                messageSource))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document remove assignee from multiple tasks`() {
    val batchRequestResource = BatchRequestResource(setOf(task1Identifier, task2Identifier))
    val unassignField = ConstrainedFields(BatchRequestResource::class.java)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(UNASSIGN_TASKS_BATCH_ENDPOINT)), batchRequestResource))
        .andExpectAll(
            status().isOk,
            jsonPath("$.tasks.length()").value(2),
            jsonPath("$.tasks[0].assignee.displayName").doesNotExist(),
            jsonPath("$.tasks[0].assignee.id").doesNotExist(),
            jsonPath("$.tasks[1].assignee.displayName").doesNotExist(),
            jsonPath("$.tasks[1].assignee.id").doesNotExist())
        .andDo(
            document(
                "tasks/document-unassign-tasks-from-project-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    unassignField
                        .withPath("ids")
                        .description("ID's of tasks which should be unassigned")
                        .type(ARRAY)),
                TASKS_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 2, false)
        .first()
        .aggregate
        .apply { assertThat(getAssigneeIdentifier()).isNull() }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 2, false)
        .second()
        .aggregate
        .apply { assertThat(getAssigneeIdentifier()).isNull() }
  }
}
