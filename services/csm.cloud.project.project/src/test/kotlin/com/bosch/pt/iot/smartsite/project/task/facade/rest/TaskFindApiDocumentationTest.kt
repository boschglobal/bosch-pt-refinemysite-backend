/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.FIND_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskFindApiDocumentationTest : AbstractTaskApiDocumentationTest() {

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
        .submitTask { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTaskSchedule()
        .submitTask(eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }
        .submitTask("task-1") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTaskSchedule()
        .submitTask("task-1", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }
        .submitTask("task-2") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTaskSchedule()
        .submitTask("task-2", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document find single task`() {
    mockMvc
        .perform(requestBuilder(get(latestVersionOf(TASK_BY_TASK_ID_ENDPOINT), task1.identifier)))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").value(task1.identifier.toString()),
            jsonPath("$.name").value(task1.name),
            jsonPath("$.location").value(task1.location))
        .andDo(
            document(
                "tasks/document-get-task-with-id",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                TASK_LINKS,
                responseHeaders(
                    headerWithName(ETAG_HEADER)
                        .description(
                            "Entity tag of the task, needed for possible updates of the task")),
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find multiple tasks`() {
    val batchRequestResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(FIND_BATCH_ENDPOINT), getIdentifier("project")),
                batchRequestResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(2))
        .andDo(
            document(
                "tasks/document-batch-find-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project that the task belongs to")),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }
}
