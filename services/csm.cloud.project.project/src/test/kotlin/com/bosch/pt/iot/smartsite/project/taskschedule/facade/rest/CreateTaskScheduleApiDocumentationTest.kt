/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardApiDocumentationTest.Companion.DESCRIPTION_LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULES_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULE_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_UPDATE_TASKSCHEDULE
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class CreateTaskScheduleApiDocumentationTest : AbstractTaskScheduleApiDocumentationTest() {

  @Test
  fun `verify and document create task schedule`() {
    eventStreamGenerator.submitTask("newTask")
    projectEventStoreUtils.reset()

    val taskId = getIdentifier("newTask")

    val requestResource = CreateTaskScheduleResource(now(), now())

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(SCHEDULE_BY_TASK_ID_ENDPOINT), taskId), requestResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    Matchers.matchesRegex(
                        ".*${
                latestVersionOf(
                  "/projects/tasks/$taskId/schedule$"
                )
              }")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(userTest),
            *isLastModifiedBy(userTest),
        )
        .andDo(
            document(
                "task-schedule/document-create-task-schedule",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the schedule belongs to")),
                saveTaskScheduleRequestFieldsSnippet,
                responseHeaders(
                    headerWithName(LOCATION).description("Location of created topic resource")),
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE)),
                taskScheduleWithoutSlotsResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED)
        .aggregate
        .also { aggregate ->
          val entity =
              repositories.findTaskScheduleWithDetails(
                  aggregate.getIdentifier().asTaskScheduleId())!!
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(aggregate, TASKSCHEDULE, userTest)
          assertThat(aggregate.start).isEqualTo(entity.start!!.toEpochMilli())
          assertThat(aggregate.end).isEqualTo(entity.end!!.toEpochMilli())
          assertThat(aggregate.task.identifier).isEqualTo(entity.task!!.identifier.toString())
          assertThat(aggregate.slots.size).isEqualTo(entity.slots!!.size)
          assertThat(aggregate.slots).isEmpty()
        }
  }

  @Test
  fun `verify and document create task schedules`() {
    eventStreamGenerator.submitTask("newTask").submitTask("newTask2")
    projectEventStoreUtils.reset()

    val taskIdentifier1 = getIdentifier("newTask")
    val taskIdentifier2 = getIdentifier("newTask2")

    val createTaskScheduleBatchResources: MutableCollection<CreateTaskScheduleBatchResource> =
        ArrayList()
    createTaskScheduleBatchResources.add(
        CreateTaskScheduleBatchResource(taskIdentifier1, startDate, startDate.plusDays(7)))
    createTaskScheduleBatchResources.add(
        CreateTaskScheduleBatchResource(taskIdentifier2, startDate, startDate.plusDays(14)))

    val requestResource: UpdateBatchRequestResource<*> =
        UpdateBatchRequestResource(createTaskScheduleBatchResources)

    mockMvc
        .perform(requestBuilder(post(latestVersionOf(SCHEDULES_BATCH_ENDPOINT)), requestResource))
        .andExpect(status().isOk)
        .andDo(
            document(
                "task-schedule/document-create-task-schedules",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                createTaskScheduleBatchResourcesRequestFieldsSnippet,
                taskScheduleWithoutSlotsListResponseFields,
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE))))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(
            TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 2, false)
        .also { events ->
          for (event in events) {
            val aggregate = event.aggregate
            val entity =
                repositories.findTaskScheduleWithDetails(
                    aggregate.getIdentifier().asTaskScheduleId())!!
            validateCreatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, TASKSCHEDULE, userTest)
            assertThat(aggregate.start).isEqualTo(entity.start!!.toEpochMilli())
            assertThat(aggregate.end).isEqualTo(entity.end!!.toEpochMilli())
            assertThat(aggregate.task.identifier).isEqualTo(entity.task!!.identifier.toString())
            assertThat(aggregate.slots.size).isEqualTo(entity.slots!!.size)
            assertThat(aggregate.slots).isEmpty()
          }
        }
  }
}
