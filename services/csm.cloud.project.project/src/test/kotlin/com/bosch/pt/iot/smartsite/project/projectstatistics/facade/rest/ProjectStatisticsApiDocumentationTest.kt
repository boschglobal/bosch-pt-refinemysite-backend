/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest.ProjectStatisticsController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest.ProjectStatisticsController.Companion.STATISTICS_BY_PROJECT_ID_ENDPOINT
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectStatisticsApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm1"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document get project statistics`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(STATISTICS_BY_PROJECT_ID_ENDPOINT), project.identifier)))
        .andExpect(status().isOk)
        .andDo(
            document(
                "projects/document-get-project-statistics",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")),
                responseFields(PROJECT_STATISTICS_RESPONSE_FIELD_DESCRIPTORS)))
  }

  companion object {

    private val PROJECT_STATISTICS_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("draftTasks")
                .description("Number of tasks in the project with DRAFT status"),
            fieldWithPath("openTasks")
                .description("Number of tasks in the project with OPEN status"),
            fieldWithPath("startedTasks")
                .description("Number of tasks in the project with STARTED status"),
            fieldWithPath("closedTasks")
                .description("Number of tasks in the project with CLOSED status"),
            fieldWithPath("acceptedTasks")
                .description("Number of tasks in the project with ACCEPTED status"),
            fieldWithPath("uncriticalTopics")
                .description("Number of topics in the project with UNCRITICAL criticality"),
            fieldWithPath("criticalTopics")
                .description("Number of topics in the project with CRITICAL criticality"),
            subsectionWithPath("_links").ignored().optional())
  }
}
