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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardApiDocumentationTest.Companion.DESCRIPTION_LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULES_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULE_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_UPDATE_TASKSCHEDULE
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class UpdateTaskScheduleApiDocumentationTest : AbstractTaskScheduleApiDocumentationTest() {

  @Test
  fun `verify and document update task schedule`() {
    eventStreamGenerator.submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
      it.start = startDate.toEpochMilli()
      it.end = startDate.plusDays(4).toEpochMilli()
    }
    projectEventStoreUtils.reset()

    val requestResource = UpdateTaskScheduleResource(now(), now(), emptyList())

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf(SCHEDULE_BY_TASK_ID_ENDPOINT), taskId), requestResource, 1L))
        .andExpect(status().isOk)
        .andExpect(header().exists("ETag"))
        .andDo(
            document(
                "task-schedule/document-update-task-schedule",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the schedule belongs to")),
                saveTaskScheduleRequestFieldsSnippet,
                requestHeaders(headerWithName(IF_MATCH).description("If-Match header with ETag")),
                responseHeaders(
                    headerWithName("ETag")
                        .description(
                            "Entity tag of the created task schedule, " +
                                "needed for possible updates of the task schedule")),
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE)),
                taskScheduleWithoutSlotsResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.UPDATED)
        .aggregate
        .also { aggregate ->
          val entity =
              repositories.findTaskScheduleWithDetails(
                  aggregate.getIdentifier().asTaskScheduleId())!!
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, entity, ProjectmanagementAggregateTypeEnum.TASKSCHEDULE)
          assertThat(aggregate.start).isEqualTo(entity.start!!.toEpochMilli())
          assertThat(aggregate.end).isEqualTo(entity.end!!.toEpochMilli())
          assertThat(aggregate.task.identifier).isEqualTo(entity.task!!.identifier.toString())
          assertThat(aggregate.slots.size).isEqualTo(entity.slots!!.size)
          assertThat(aggregate.slots).isEmpty()
        }
  }

  @Test
  fun `verify and document update task schedules`() {
    eventStreamGenerator
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(4).toEpochMilli()
        }
        .submitTask("task2")
        .submitTaskSchedule("taskSchedule2")
        .submitTaskSchedule("taskSchedule2", eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(4).toEpochMilli()
        }
    projectEventStoreUtils.reset()

    val taskIdentifier1 = getIdentifier("task")
    val taskIdentifier2 = getIdentifier("task2")

    val updateTaskScheduleBatchResources: MutableCollection<UpdateTaskScheduleBatchResource> =
        ArrayList()
    updateTaskScheduleBatchResources.add(
        UpdateTaskScheduleBatchResource(1L, taskIdentifier1, now(), now().plusDays(7), emptyList()))
    updateTaskScheduleBatchResources.add(
        UpdateTaskScheduleBatchResource(
            1L, taskIdentifier2, now(), now().plusDays(14), emptyList()))

    val requestResource: UpdateBatchRequestResource<*> =
        UpdateBatchRequestResource(updateTaskScheduleBatchResources)

    mockMvc
        .perform(requestBuilder(put(latestVersionOf(SCHEDULES_BATCH_ENDPOINT)), requestResource))
        .andExpect(status().isOk)
        .andDo(
            document(
                "task-schedule/document-update-task-schedules",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                updateTaskScheduleBatchResourcesRequestFieldsSnippet,
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
            TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.UPDATED, 2, false)
        .also { events ->
          for (event in events) {
            val aggregate = event.aggregate
            val entity =
                repositories.findTaskScheduleWithDetails(
                    aggregate.getIdentifier().asTaskScheduleId())!!
            validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, entity, ProjectmanagementAggregateTypeEnum.TASKSCHEDULE)
            assertThat(aggregate.start).isEqualTo(entity.start!!.toEpochMilli())
            assertThat(aggregate.end).isEqualTo(entity.end!!.toEpochMilli())
            assertThat(aggregate.task.identifier).isEqualTo(entity.task!!.identifier.toString())
            assertThat(aggregate.slots.size).isEqualTo(entity.slots!!.size)
            assertThat(aggregate.slots).isEmpty()
          }
        }
  }
}
