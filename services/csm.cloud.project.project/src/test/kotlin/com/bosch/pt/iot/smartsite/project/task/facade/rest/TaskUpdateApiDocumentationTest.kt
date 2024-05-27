/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.SaveTaskResourceBuilder
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.user.model.User
import com.google.common.net.HttpHeaders.IF_MATCH
import java.util.Locale.GERMAN
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.headers.HeaderDocumentation
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskUpdateApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  private val fmParticipantIdentifier by lazy { getIdentifier("fm-participant").asParticipantId() }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val workAreaIdentifier2: UUID by lazy { getIdentifier("workArea2") }
  private val task1Identifier: TaskId by lazy { getIdentifier("task-1").asTaskId() }
  private val task2Identifier: TaskId by lazy { getIdentifier("task-2").asTaskId() }

  private val csmUser: User by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("csm-user"))!!
  }

  private val company: Company by lazy {
    repositories.companyRepository.findOneByIdentifier(getIdentifier("company"))!!
  }

  private val project: Project by lazy {
    repositories.projectRepository.findOneByIdentifier(projectIdentifier)!!
  }

  private val fmParticipant: Participant by lazy {
    repositories.participantRepository.findOneWithDetailsByIdentifier(fmParticipantIdentifier)!!
  }

  private val projectCraft2: ProjectCraft by lazy {
    repositories.projectCraftRepository.findOneByIdentifier(
        getIdentifier("projectCraft2").asProjectCraftId())!!
  }

  private val workArea2: WorkArea by lazy {
    repositories.workAreaRepository.findOneByIdentifier(workAreaIdentifier2.asWorkAreaId())!!
  }

  private val task1: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(task1Identifier)!!
  }

  private val task2: Task by lazy {
    repositories.taskRepository.findOneByIdentifier(task2Identifier)!!
  }

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitWorkArea()
        .submitWorkArea("workArea2")
        .submitWorkAreaList()
        .submitProjectCraftG2("projectCraft2")
        .submitTask("task-1") { it.status = DRAFT }
        .submitTask("task-2") { it.status = DRAFT }
        .submitTask("task-1", eventType = SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }
        .submitTask("task-2", eventType = SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document updating of a task`() {
    val resource =
        SaveTaskResourceBuilder()
            .setName("Task name updated")
            .setDescription("Task description updated")
            .setLocation("Updated location")
            .setProjectCraftId(projectCraft2.identifier)
            .setWorkAreaId(workAreaIdentifier2.asWorkAreaId())
            .setProjectId(projectIdentifier)
            .setAssigneeId(fmParticipantIdentifier)
            .setStatus(OPEN)
            .createSaveTaskResource()

    val expectedVersion = 2L

    mockMvc
        .perform(
            requestBuilder(
                    put(latestVersionOf(TASK_BY_TASK_ID_ENDPOINT), task1Identifier), resource, 1L)
                .locale(GERMAN))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"$expectedVersion\""),
            *hasIdentifierAndVersion(task1Identifier.toUuid(), expectedVersion),
            *isCreatedBy(csmUser),
            *isLastModifiedBy(csmUser),
            jsonPath("$.name").value(resource.name),
            jsonPath("$.location").value(resource.location),
            jsonPath("$.description").value(resource.description),
            jsonPath("$.projectCraft.id").value(resource.projectCraftId.toString()),
            jsonPath("$.projectCraft.name").value(projectCraft2.name),
            jsonPath("$.projectCraft.color").value(projectCraft2.color),
            jsonPath("$.project.id").value(project.identifier.toString()),
            jsonPath("$.project.displayName").value(project.getDisplayName()),
            jsonPath("$.company.id").value(company.identifier.toString()),
            jsonPath("$.company.displayName").value(company.getDisplayName()),
            jsonPath("$.workArea.id").value(resource.workAreaId.toString()),
            jsonPath("$.workArea.displayName").value(workArea2.getDisplayName()),
            jsonPath("$.status").value(OPEN.toString()),
        )
        .andDo(
            document(
                "tasks/document-update-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                HeaderDocumentation.requestHeaders(
                    HeaderDocumentation.headerWithName(IF_MATCH)
                        .description(
                            "Mandatory entity tag of the task to be updated (previously received value of the " +
                                "response header field `ETag`)")),
                requestFields(SAVE_TASK_REQUEST_FIELDS),
                TASK_LINKS,
                responseHeaders(
                    HeaderDocumentation.headerWithName(ETAG_HEADER)
                        .description(
                            "Entity tag of the created task, needed for possible further updates of the task")),
                responseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, UPDATED, 1, true)
        .first()
        .aggregate
        .apply {
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              this, repositories.findTaskWithDetails(getIdentifier().asTaskId())!!, TASK)
          verifyUpdatedAggregate(this, resource)
        }
  }

  @Test
  fun `verify updating single unassigned task with assignee`() {
    eventStreamGenerator.submitTask("unassigned task") {
      it.assignee = null
      it.status = DRAFT
    }

    val aggregate = get<TaskAggregateAvro>("unassigned task")!!

    val resource =
        SaveTaskResourceBuilder()
            .setName("Updated task name")
            .setDescription(aggregate.description)
            .setLocation(aggregate.location)
            .setProjectCraftId(aggregate.getCraftIdentifier().asProjectCraftId())
            .setWorkAreaId(aggregate.getWorkAreaIdentifier()?.asWorkAreaId())
            .setProjectId(aggregate.getProjectIdentifier().asProjectId())
            .setAssigneeId(fmParticipantIdentifier)
            .setStatus(TaskStatusEnum.DRAFT)
            .createSaveTaskResource()

    mockMvc
        .perform(
            requestBuilder(
                    put(latestVersionOf(TASK_BY_TASK_ID_ENDPOINT), aggregate.getIdentifier()),
                    resource,
                    0L)
                .locale(GERMAN))
        .andExpectAll(
            status().isOk,
            *hasIdentifierAndVersion(aggregate.getIdentifier(), 2L),
            jsonPath("$.assignee.id").value(fmParticipant.identifier.toString()),
            jsonPath("$.assignee.displayName").value(fmParticipant.getDisplayName()),
            jsonPath("$.status").value(DRAFT.toString()))

    projectEventStoreUtils.verifyContainsInSequence(
        TaskEventAvro::class.java, TaskEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, UPDATED, 1, false)
        .first()
        .aggregate
        .apply { assertThat(aggregateIdentifier.version).isEqualTo(1) }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, ASSIGNED, 1, false)
        .first()
        .aggregate
        .apply {
          assertThat(aggregateIdentifier.version).isEqualTo(2)
          assertThat(getAssigneeIdentifier()?.asParticipantId()).isEqualTo(resource.assigneeId)
        }
  }

  @Test
  fun `verify updating single unassigned task with assignee and status open`() {
    eventStreamGenerator.submitTask("unassigned task") {
      it.assignee = null
      it.status = DRAFT
    }

    val aggregate = get<TaskAggregateAvro>("unassigned task")!!

    val resource =
        SaveTaskResourceBuilder()
            .setName("Updated task name")
            .setDescription(aggregate.description)
            .setLocation(aggregate.location)
            .setProjectCraftId(aggregate.getCraftIdentifier().asProjectCraftId())
            .setWorkAreaId(aggregate.getWorkAreaIdentifier()?.asWorkAreaId())
            .setProjectId(aggregate.getProjectIdentifier().asProjectId())
            .setAssigneeId(fmParticipantIdentifier)
            .setStatus(OPEN)
            .createSaveTaskResource()

    mockMvc
        .perform(
            requestBuilder(
                    put(latestVersionOf(TASK_BY_TASK_ID_ENDPOINT), aggregate.getIdentifier()),
                    resource,
                    0L)
                .locale(GERMAN))
        .andExpectAll(
            status().isOk,
            *hasIdentifierAndVersion(aggregate.getIdentifier(), 3L),
            jsonPath("$.assignee.id").value(fmParticipant.identifier.toString()),
            jsonPath("$.assignee.displayName").value(fmParticipant.getDisplayName()),
            jsonPath("$.status").value(OPEN.toString()))

    projectEventStoreUtils.verifyContainsInSequence(
        TaskEventAvro::class.java, TaskEventAvro::class.java, TaskEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, UPDATED, 1, false)
        .first()
        .aggregate
        .apply { assertThat(aggregateIdentifier.version).isEqualTo(1) }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, ASSIGNED, 1, false)
        .first()
        .aggregate
        .apply {
          assertThat(aggregateIdentifier.version).isEqualTo(2)
          assertThat(getAssigneeIdentifier()?.asParticipantId()).isEqualTo(resource.assigneeId)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, SENT, 1, false)
        .first()
        .aggregate
        .apply {
          assertThat(aggregateIdentifier.version).isEqualTo(3)
          assertThat(status.name).isEqualTo(resource.status.name)
        }
  }

  @Test
  fun `verify and document updating multiple tasks`() {
    val resource1 =
        SaveTaskResourceWithIdentifierAndVersion(
            task1.identifier.toUuid(),
            task1.version,
            "Updated Task 1",
            "Task description updated",
            "Updated location",
            OPEN,
            projectIdentifier,
            projectCraft2.identifier,
            fmParticipantIdentifier,
            getIdentifier("workArea").asWorkAreaId())

    val resource2 =
        SaveTaskResourceWithIdentifierAndVersion(
            task2.identifier.toUuid(),
            task2.version,
            "Updated Task 2",
            "Task description updated",
            "Updated location",
            OPEN,
            projectIdentifier,
            projectCraft2.identifier,
            fmParticipantIdentifier,
            getIdentifier("workArea").asWorkAreaId())

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf(TASKS_BATCH_ENDPOINT)),
                UpdateBatchRequestResource(listOf(resource1, resource2))))
        .andExpect(status().isOk)
        .andDo(
            document(
                "tasks/document-update-multiple-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                UPDATE_BATCH_RESOURCE_REQUEST_FIELDS_SNIPPET,
                TASKS_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskEventAvro::class.java,
        TaskEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java,
        // no assignment update
        // no status update (started)
    )

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, UPDATED, 2, false).also {
      it.first().aggregate.apply {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, repositories.findTaskWithDetails(getIdentifier().asTaskId())!!, TASK)
        verifyUpdatedAggregate(this, resource1)
      }
      it.second().aggregate.apply {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, repositories.findTaskWithDetails(getIdentifier().asTaskId())!!, TASK)
        verifyUpdatedAggregate(this, resource2)
      }
    }
  }

  private fun verifyUpdatedAggregate(aggregate: TaskAggregateAvro, resource: SaveTaskResource) =
      with(aggregate) {
        assertThat(getAssigneeIdentifier()?.asParticipantId()).isEqualTo(resource.assigneeId)
        assertThat(getCraftIdentifier().asProjectCraftId()).isEqualTo(resource.projectCraftId)
        assertThat(description).isEqualTo(resource.description)
        assertThat(name).isEqualTo(resource.name)
        assertThat(getProjectIdentifier().asProjectId()).isEqualTo(resource.projectId)
        assertThat(location).isEqualTo(resource.location)
        assertThat(status.name).isEqualTo(resource.status.name)
        assertThat(getWorkAreaIdentifier()).isEqualTo(resource.workAreaId!!.toUuid())
      }
}
