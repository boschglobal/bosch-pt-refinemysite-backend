/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyParameters
import com.bosch.pt.iot.smartsite.project.copy.facade.rest.ProjectCopyController.Companion.COPY_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.copy.facade.rest.ProjectCopyController.Companion.PATH_VARIABLE_PROJECT_ID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectCopyApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .setupDatasetTestData()
        .setUserContext("userCsm1")

    setAuthentication(eventStreamGenerator.getIdentifier("userCsm1"))
    commandSendingService.clearRecords()
  }

  @Test
  fun `verify and document project copy`() {
    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(COPY_BY_PROJECT_ID_ENDPOINT), getIdentifier("project")),
                copyParameters))
        .andExpectAll(
            status().isAccepted, content().contentType(HAL_JSON_VALUE), jsonPath("$.id").exists())
        .andDo(
            document(
                "project-copy/document-project-copy",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description(PATH_VARIABLE_PROJECT_ID_DESCRIPTION)),
                requestFields(COPY_PARAMETERS_REQUEST_FIELDS),
                responseFields(COPY_RESOURCE_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `returns BAD_REQUEST on invalid ProjectCopyParameters configuration`() {
    mockMvc
        .perform(
            requestBuilder(
                    post(latestVersionOf(COPY_BY_PROJECT_ID_ENDPOINT), getIdentifier("project")),
                )
                .contentType(APPLICATION_JSON)
                .content(
                    """{
                      |  "projectName": "Copied Project",
                      |  "workingAreas": true,
                      |  "disciplines": true,
                      |  "milestones": true,
                      |  "tasks": false,
                      |  "dayCards": true,
                      |  "topics": true,
                      |  "keepTaskStatus": true,
                      |  "keepTaskAssignee":true
                      |}"""
                        .trimMargin()))
        .andExpectAll(
            status().isBadRequest,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.message").value("An invalid request was sent to the server."))
  }

  companion object {
    private val copyParameters =
        ProjectCopyParameters(
            "New project name",
            workingAreas = true,
            disciplines = true,
            milestones = true,
            tasks = true,
            dayCards = true,
            keepTaskStatus = true,
            keepTaskAssignee = true)

    private const val PATH_VARIABLE_PROJECT_ID_DESCRIPTION = "ID of the project to copy"

    private val COPY_PARAMETERS_CONSTRAINED_FIELDS =
        ConstrainedFields(ProjectCopyParameters::class.java)

    private val COPY_PARAMETERS_REQUEST_FIELDS =
        listOf(
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("projectName")
                .description("Name of the new project")
                .type(STRING),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("workingAreas")
                .description("Working areas should also be copied")
                .type(BOOLEAN),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("disciplines")
                .description("Disciplines (crafts) should also be copied")
                .type(BOOLEAN),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("milestones")
                .description("Milestones should also be copied")
                .type(BOOLEAN),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("tasks")
                .description("Tasks should also be copied")
                .type(BOOLEAN),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("dayCards")
                .description("Day cards should also be copied")
                .type(BOOLEAN),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("keepTaskStatus")
                .description("The tasks' status should be kept while copying")
                .type(BOOLEAN),
            COPY_PARAMETERS_CONSTRAINED_FIELDS.withPath("keepTaskAssignee")
                .description("The tasks' assignee should be kept while copying")
                .type(BOOLEAN),
        )

    private val COPY_RESOURCE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(fieldWithPath("id").description("The ID of the async copy job").type(STRING))
  }
}
