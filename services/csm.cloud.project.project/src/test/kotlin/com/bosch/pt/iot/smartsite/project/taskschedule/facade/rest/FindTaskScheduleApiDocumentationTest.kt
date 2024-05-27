/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK_SCHEDULE
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardApiDocumentationTest.Companion.DESCRIPTION_LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.PATH_VARIABLE_SCHEDULE_ID
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULES_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULE_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_UPDATE_TASKSCHEDULE
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class FindTaskScheduleApiDocumentationTest : AbstractTaskScheduleApiDocumentationTest() {

  @Test
  fun `verify and document find by identifier`() {
    eventStreamGenerator
        .submitDayCardG2()
        .submitDayCardG2("dayCard2") { it.status = DayCardStatusEnumAvro.DONE }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(4).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(startDate.toEpochMilli(), getByReference("dayCard")),
                  TaskScheduleSlotAvro(
                      startDate.plusDays(1).toEpochMilli(), getByReference("dayCard2")),
              )
        }

    projectEventStoreUtils.reset()

    val projectIdentifier = getIdentifier("project")
    val scheduleIdentifier = getIdentifier("taskSchedule")

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf("/projects/{projectId}/tasks/schedules/{scheduleId}"),
                    projectIdentifier,
                    scheduleIdentifier)))
        .andExpectAll(status().isOk, header().string(ETAG_HEADER, "\"1\""))
        .andDo(
            document(
                "task-schedule/document-get-task-schedule-by-identifier-and-project-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project the schedule belongs to"),
                    parameterWithName(PATH_VARIABLE_SCHEDULE_ID)
                        .description("ID of the schedule belongs to")),
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
                taskScheduleResponseFields))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find by task identifier`() {
    eventStreamGenerator
        .submitDayCardG2()
        .submitDayCardG2("dayCard2") { it.status = DayCardStatusEnumAvro.DONE }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(4).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(startDate.toEpochMilli(), getByReference("dayCard")),
                  TaskScheduleSlotAvro(
                      startDate.plusDays(1).toEpochMilli(), getByReference("dayCard2")),
              )
        }

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf(SCHEDULE_BY_TASK_ID_ENDPOINT), taskId), null, 1L))
        .andExpect(status().isOk)
        .andExpect(header().string(ETAG_HEADER, "\"1\""))
        .andDo(
            document(
                "task-schedule/document-get-task-schedule",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the schedule belongs to")),
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
                taskScheduleResponseFields))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find by task identifiers`() {
    eventStreamGenerator
        .submitDayCardG2()
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(7).toEpochMilli()
          it.slots =
              listOf(TaskScheduleSlotAvro(startDate.toEpochMilli(), getByReference("dayCard")))
        }
        .submitTask("task2")
        .submitTaskSchedule("taskSchedule2")

    projectEventStoreUtils.reset()

    val task1Identifier = getIdentifier("task")
    val task2Identifier = getIdentifier("task2")

    val requestResource = BatchRequestResource(setOf(task1Identifier, task2Identifier))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(SCHEDULES_ENDPOINT))
                    .locale(DEFAULT_LOCALE)
                    .header(ACCEPT, HAL_JSON_VALUE)
                    .header(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE_EN)
                    .header(ACCEPT_LANGUAGE, DEFAULT_LANGUAGE)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(ObjectMapper().writeValueAsString(requestResource))
                    .accept(MediaType.parseMediaType(HAL_JSON_VALUE))
                    .param("identifierType", TASK)))
        .andExpect(status().isOk)
        .andDo(
            document(
                "task-schedule/document-search-task-schedules",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(TASK, TASK),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                searchTaskSchedulesResponseFields,
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find by task schedule identifiers`() {
    eventStreamGenerator
        .submitDayCardG2()
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(7).toEpochMilli()
          it.slots =
              listOf(TaskScheduleSlotAvro(startDate.toEpochMilli(), getByReference("dayCard")))
        }
        .submitTask("task2")
        .submitTaskSchedule("taskSchedule2")

    projectEventStoreUtils.reset()

    val taskSchedule1Identifier = getIdentifier("taskSchedule")
    val taskSchedule2Identifier = getIdentifier("taskSchedule2")

    val requestResource =
        BatchRequestResource(setOf(taskSchedule1Identifier, taskSchedule2Identifier))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(SCHEDULES_ENDPOINT))
                    .locale(DEFAULT_LOCALE)
                    .header(ACCEPT, HAL_JSON_VALUE)
                    .header(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE_EN)
                    .header(ACCEPT_LANGUAGE, DEFAULT_LANGUAGE)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(ObjectMapper().writeValueAsString(requestResource))
                    .accept(MediaType.parseMediaType(HAL_JSON_VALUE))
                    .param("identifierType", TASK_SCHEDULE)))
        .andExpect(status().isOk)
        .andDo(
            document(
                "task-schedule/document-search-task-schedules",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(TASK_SCHEDULE, TASK),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                searchTaskSchedulesResponseFields,
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE))))

    projectEventStoreUtils.verifyEmpty()
  }
}
