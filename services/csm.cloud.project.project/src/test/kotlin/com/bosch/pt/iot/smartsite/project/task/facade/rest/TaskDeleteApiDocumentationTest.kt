/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.DELETE_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskDeleteApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  private val task1: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(getIdentifier("task1").asTaskId())!!
  }

  private val task2: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(getIdentifier("task2").asTaskId())!!
  }

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitTask(asReference = "task1") { it.status = DRAFT }
        .submitTask(asReference = "task2") { it.status = OPEN }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and documents the deletion of a task`() {
    mockMvc
        .perform(
            requestBuilder(
                delete(latestVersionOf(TASK_BY_TASK_ID_ENDPOINT), getIdentifier("task1"))))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "tasks/document-delete-task-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task"))))

    // No event is sent since this is an async operation that triggers
    // a command sent via kafka first
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and documents the deletion of multiple tasks`() {
    val tasksResource =
        BatchRequestResource(setOf(task1.identifier.toUuid(), task2.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(DELETE_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                tasksResource))
        .andExpectAll(status().isNoContent)
        .andDo(
            document(
                "tasks/document-delete-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())))

    // No event is sent since this is an asynch operation that triggers
    // a command sent via kafka first
    projectEventStoreUtils.verifyEmpty()
  }

  /*
  The aim of this test is to check whether the deletion of multiple tasks causes
   any performance issues.
   To ensure that our build pipelines remain efficient, this test has been disabled.

   1 Task:
   Start time: 2023-08-02T08:14:10.321904
   End time: 2023-08-02T08:14:10.516362

   500 Tasks:
   Start time: 2023-08-02T10:40:01.990361
   End time: 2023-08-02T10:40:02.868846

   PS: The endpoint has already been limited to a maximum of 100 tasks.
  */
  @Test
  @Disabled
  fun `verify and documents the deletion of 500 tasks`() {
    val tasks = mutableSetOf<UUID>()

    for (i in 1..500) {
      val taskName = "task$i"
      val task = eventStreamGenerator.submitTask(asReference = taskName) { it.status = DRAFT }

      tasks.add(task.getIdentifier(taskName))
    }

    val tasksResource = BatchRequestResource(tasks)

    LOGGER.warn("Start time: " + LocalDateTime.now())

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(DELETE_TASKS_BATCH_ENDPOINT),
                    getIdentifier("project").asProjectId()),
                tasksResource))
        .andExpectAll(status().isNoContent)
        .andDo(
            document(
                "tasks/document-delete-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())))

    LOGGER.warn("End time: " + LocalDateTime.now())

    // No event is sent since this is an asynch operation that triggers
    // a command sent via kafka first
    projectEventStoreUtils.verifyEmpty()
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(TaskDeleteApiDocumentationTest::class.java)
  }
}
