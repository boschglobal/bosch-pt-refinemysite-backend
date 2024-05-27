/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest.TaskStatisticsController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest.TaskStatisticsController.Companion.STATISTICS_BY_TASK_ID_ENDPOINT
import java.util.Locale
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Test Task Statistics API")
@EnableAllKafkaListeners
class TaskStatisticsApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun verifyAndDocumentGetTaskStatistics() {

    mockMvc
        .perform(
            get(latestVersionOf(STATISTICS_BY_TASK_ID_ENDPOINT), getIdentifier("task"))
                .locale(Locale.ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, "en")
                .contentType(APPLICATION_JSON_VALUE)
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isOk)
        .andDo(
            document(
                "tasks/document-get-task-statistics",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                responseFields(
                    fieldWithPath("uncriticalTopics")
                        .description("Number of uncritical topics in the task"),
                    fieldWithPath("criticalTopics")
                        .description("Number of critical topics in the task"),
                    subsectionWithPath("_links").ignored().optional())))
  }
}
