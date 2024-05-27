/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportCommand
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobContext
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobType.PROJECT_EXPORT
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.Companion.EXPORT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.exporter.submitProjectExportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import java.util.Locale
import org.apache.avro.specific.SpecificRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
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
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectExportApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @Autowired private lateinit var jobJsonSerializer: JobJsonSerializer

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .setupDatasetTestData()
        .submitProjectExportFeatureToggle()
        .setUserContext("userCsm1")

    setAuthentication(eventStreamGenerator.getIdentifier("userCsm1"))
    commandSendingService.clearRecords()
  }

  @Test
  fun `verify and document export ms project file`() {
    val projectIdentifier = getIdentifier("project").asProjectId()
    val project = repositories.findProject(projectIdentifier)

    projectEventStoreUtils.reset()

    val exportParameters =
        ProjectExportParameters(MS_PROJECT_XML, includeMilestones = false, includeComments = false)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(EXPORT_BY_PROJECT_ID_ENDPOINT), projectIdentifier.toUuid()),
                exportParameters))
        .andExpectAll(
            status().isAccepted, content().contentType(HAL_JSON_VALUE), jsonPath("$.id").exists())
        .andDo(
            document(
                "project-export/document-project-export",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description(PATH_VARIABLE_PROJECT_ID_DESCRIPTION)),
                requestFields(EXPORT_PARAMETERS_REQUEST_FIELDS),
                responseFields(EXPORT_RESOURCE_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()

    // TODO: [SMAR-16375] Check that the job is really triggered by evaluating the export command.
    // Check example in ProjectImportApiDocumentationTest
    assertThat(commandSendingService.capturedRecords).hasSize(1)
    commandSendingService.capturedRecords.first().value.apply {
      assertJobTypeAndIdentifier(this)
      assertContext(this, checkNotNull(project))
      assertExportCommand(this, projectIdentifier)
    }
  }

  private fun assertJobTypeAndIdentifier(message: SpecificRecord) {
    assertThat(message).isInstanceOf(EnqueueJobCommandAvro::class.java)
    (message as EnqueueJobCommandAvro).apply {
      assertThat(jobType).isEqualTo(PROJECT_EXPORT.name)
      assertThat(this.aggregateIdentifier.type).isEqualTo("JOB")
    }
  }

  private fun assertContext(message: SpecificRecord, project: Project) {
    (message as EnqueueJobCommandAvro).jsonSerializedContext.apply {
      ProjectExportJobContext(ResourceReference.from(project))
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(type).isEqualTo(this.type)
            assertThat(json).isEqualTo(this.json)
          }
    }
  }

  private fun assertExportCommand(message: SpecificRecord, projectIdentifier: ProjectId) {
    (message as EnqueueJobCommandAvro).jsonSerializedCommand.apply {
      ProjectExportCommand(
              Locale.UK,
              projectIdentifier.toUuid(),
              ProjectExportParameters(
                  MS_PROJECT_XML, includeMilestones = false, includeComments = false))
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  companion object {
    private const val PATH_VARIABLE_PROJECT_ID_DESCRIPTION = "ID of the project to export"

    private val EXPORT_PARAMETERS_CONSTRAINED_FIELDS =
        ConstrainedFields(ProjectExportParameters::class.java)

    private val EXPORT_PARAMETERS_REQUEST_FIELDS =
        listOf(
            EXPORT_PARAMETERS_CONSTRAINED_FIELDS.withPath("format")
                .description("Export format")
                .attributes(
                    key("constraints")
                        .value("One of: ${ProjectExportFormatEnum.values().map { it.name }}"))
                .type(STRING),
            EXPORT_PARAMETERS_CONSTRAINED_FIELDS.withPath("includeMilestones")
                .description("Export should include milestones")
                .type(BOOLEAN),
            EXPORT_PARAMETERS_CONSTRAINED_FIELDS.withPath("includeComments")
                .description("Export should include comments"),
            EXPORT_PARAMETERS_CONSTRAINED_FIELDS.withPath("taskExportSchedulingType")
                .optional()
                .description(
                    "Export scheduling type for tasks. One of AUTO_SCHEDULED or MANUALLY_SCHEDULED. " +
                        "Defaults to AUTO_SCHEDULED.")
                .type(STRING),
            EXPORT_PARAMETERS_CONSTRAINED_FIELDS.withPath("milestoneExportSchedulingType")
                .optional()
                .description(
                    "Export scheduling type for milestones. One of AUTO_SCHEDULED or MANUALLY_SCHEDULED. " +
                        "Defaults to MANUALLY_SCHEDULED")
                .type(STRING),
        )

    private val EXPORT_RESOURCE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(fieldWithPath("id").description("The ID of the async export job").type(STRING))
  }
}
