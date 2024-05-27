/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest.Companion.TASK_RESPONSE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest.resource.request.CopyTaskResource
import com.bosch.pt.iot.smartsite.project.taskcopy.shared.dto.OverridableTaskParametersDto
import com.bosch.pt.iot.smartsite.project.taskcopy.util.TaskCopyTestUtil.assertCreatedTaskMatchCopiedTask
import com.bosch.pt.iot.smartsite.project.taskcopy.util.TaskCopyTestUtil.assertCreatedTaskScheduleMatchCopiedTaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.time.LocalDate.now
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class TaskCopyApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task1").asTaskId())!! }
  private val schedule by lazy {
    repositories.findTaskScheduleWithDetails(getIdentifier("schedule1").asTaskScheduleId())!!
  }
  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea1").asWorkAreaId())!!
  }

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task1") { it.name = "task1" }
        .submitTaskSchedule(asReference = "schedule1") {
          it.start = now().toEpochMilli()
          it.end = now().plusDays(10).toEpochMilli()
        }
        .submitDayCardG2(asReference = "dayCard1") { it.status = DONE }
        .submitTaskSchedule(asReference = "schedule1", eventType = UPDATED) {
          it.slots = listOf(getByReference("dayCard1").asSlot(now()))
        }
        .submitWorkArea(asReference = "workArea1")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document copy tasks`() {
    val parametersDto =
        OverridableTaskParametersDto(workAreaId = WorkAreaIdOrEmpty(workArea.identifier))
    val resource =
        CopyTaskResource(
            id = task.identifier,
            shiftDays = 7L,
            includeDayCards = true,
            parametersOverride = parametersDto)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/{projectId}/tasks/copy"), project.identifier),
                CreateBatchRequestResource(listOf(resource))))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(1))
        .andDo(
            document(
                "task-copy/document-task-copy",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_ID_PATH_PARAMETER_DESCRIPTOR),
                buildCreateBatchRequestFields(COPY_TASK_REQUEST_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(TASK_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            // For each Schedule:
            // - Create Day Cards Batch Operations
            // - Add of each Day Card to Schedule
            DayCardEventG2Avro::class.java,
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))

    // The new task identifier is being retrieve from the event store
    val taskIdentifier =
        projectEventStoreUtils
            .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 1, false)
            .first()
            .aggregate
            .getIdentifier()
    val copyTask = repositories.findTaskWithDetails(taskIdentifier.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyTask, task, parametersDto)

    // The new task schedule identifier is being retrieve from the event store
    val taskScheduleIdentifier =
        projectEventStoreUtils
            .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
            .first()
            .aggregate
            .getIdentifier()
    val copyTaskSchedule =
        repositories.findTaskScheduleWithDetails(taskScheduleIdentifier.asTaskScheduleId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyTaskSchedule, schedule, 7, true)
  }

  companion object {

    private val PROJECT_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")

    private val COPY_TASK_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CopyTaskResource::class.java)
                .withPath("id")
                .description("ID of the task to copy")
                .type(STRING),
            ConstrainedFields(CopyTaskResource::class.java)
                .withPath("shiftDays")
                .description("Number of shifted days from the original task to create the copy")
                .type(NUMBER),
            ConstrainedFields(CopyTaskResource::class.java)
                .withPath("includeDayCards")
                .description("Boolean indicating if day cards should be included in the copy")
                .type(BOOLEAN),
            ConstrainedFields(CopyTaskResource::class.java)
                .withPath("parametersOverride")
                .description(
                    "Task parameters that should be substituted in the copy (currently only supports work areas)")
                .type(OBJECT)
                .optional(),
            ConstrainedFields(CopyTaskResource::class.java)
                .withPath("parametersOverride.workAreaId")
                .description("Work Area Id to use in the task copy")
                .type(STRING)
                .optional())
  }
}
