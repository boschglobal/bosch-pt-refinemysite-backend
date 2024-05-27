/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskassignment

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.AssignTaskListToParticipantResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.AssignTaskToParticipantResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ASSIGN_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ASSIGN_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
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
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskAssignmentApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  private val userFm: User by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("fm-user"))!!
  }

  private val task1: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(getIdentifier("task-1").asTaskId())!!
  }

  private val participantIdentifier: ParticipantId by lazy {
    getIdentifier("fm-participant").asParticipantId()
  }
  private val task1Identifier: UUID by lazy { getIdentifier("task-1") }
  private val task2Identifier: UUID by lazy { getIdentifier("task-2") }

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitTask("task-1") { it.status = DRAFT }
        .submitTask("task-2") { it.status = DRAFT }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document assign a single task to a participant`() {
    val assignField = ConstrainedFields(AssignTaskToParticipantResource::class.java)
    val assignResource = AssignTaskToParticipantResource(participantIdentifier)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(ASSIGN_TASK_BY_TASK_ID_ENDPOINT), task1Identifier),
                assignResource))
        .andExpectAll(
            status().isOk,
            jsonPath("$.status").value(OPEN.name),
            jsonPath("$.assignee.displayName").value(userFm.getDisplayName()),
            jsonPath("$.assignee.id").value(participantIdentifier.toString()),
        )
        .andDo(
            document(
                "tasks/document-assign-task-to-project-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                TASK_LINKS,
                requestFields(
                    assignField
                        .withPath("assigneeId")
                        .description("ID of the assigned project participant")
                        .type(STRING)),
                responseHeaders(
                    headerWithName(ETAG_HEADER)
                        .description(
                            "Entity tag of the assigned task, needed for possible updates of the task")),
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, ASSIGNED, 1, false)
        .first()
        .aggregate
        .apply {
          assertThat(aggregateIdentifier.version).isEqualTo(task1.version.minus(1))
          assertThat(getIdentifier()).isEqualTo(task1Identifier)
          assertThat(status.name).isEqualTo(DRAFT.name)
          assertThat(getAssigneeIdentifier()?.asParticipantId()).isEqualTo(participantIdentifier)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, SENT, 1, false)
        .first()
        .aggregate
        .apply {
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              this,
              repositories.findTaskWithDetails(getIdentifier().asTaskId())!!,
              ProjectmanagementAggregateTypeEnum.TASK)
          assertThat(getIdentifier()).isEqualTo(task1Identifier)
          assertThat(status.name).isEqualTo(TaskStatusEnumAvro.OPEN.name)
          assertThat(getAssigneeIdentifier()?.asParticipantId()).isEqualTo(participantIdentifier)
        }
  }

  @Test
  fun `verify and document assign multiple tasks to a participant`() {
    val tasks =
        AssignTaskListToParticipantResource(
            listOf(task1Identifier, task2Identifier), participantIdentifier)
    val assignField = ConstrainedFields(AssignTaskListToParticipantResource::class.java)

    mockMvc
        .perform(requestBuilder(post(latestVersionOf(ASSIGN_TASKS_BATCH_ENDPOINT)), tasks))
        .andExpectAll(
            status().isOk,
            jsonPath("$.tasks.length()").value(2),
            jsonPath("$.tasks[0].assignee.displayName").value(userFm.getDisplayName()),
            jsonPath("$.tasks[0].assignee.id").value(participantIdentifier.toString()),
            jsonPath("$.tasks[1].assignee.displayName").value(userFm.getDisplayName()),
            jsonPath("$.tasks[1].assignee.id").value(participantIdentifier.toString()))
        .andDo(
            document(
                "tasks/document-assign-tasks-to-project-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    assignField
                        .withPath("taskIds")
                        .description("ID's of tasks which should be changed")
                        .type(ARRAY),
                    assignField
                        .withPath("assigneeId")
                        .description("ID of the assigned project participant")
                        .type(STRING)),
                TASKS_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, ASSIGNED, 2, false)
    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, SENT, 2, false)
  }
}
