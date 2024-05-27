/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULE_BY_TASK_ID_ENDPOINT
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class DeleteTaskScheduleApiDocumentationTest : AbstractTaskScheduleApiDocumentationTest() {

  @Test
  fun `verify and document delete by task identifier`() {
    val taskSchedule =
        repositories.findTaskScheduleWithDetails(getIdentifier("taskSchedule").asTaskScheduleId())!!

    mockMvc
        .perform(
            requestBuilder(delete(latestVersionOf(SCHEDULE_BY_TASK_ID_ENDPOINT), taskId), null, 0L))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "task-schedule/document-delete-task-schedule",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the schedule belongs to")),
                requestHeaders(headerWithName(IF_MATCH).description("If-Match header with ETag"))))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.DELETED)
        .aggregate
        .also {
          validateDeletedAggregateAuditInfoAndAggregateIdentifier(
              it, taskSchedule, ProjectmanagementAggregateTypeEnum.TASKSCHEDULE, userTest)
        }
  }
}
