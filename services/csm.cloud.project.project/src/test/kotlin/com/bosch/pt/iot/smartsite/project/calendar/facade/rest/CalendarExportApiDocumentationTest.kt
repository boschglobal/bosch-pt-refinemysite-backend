/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.project.calendar.api.AssigneesFilter
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsCsvCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsJsonCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsPdfCommand
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_CSV
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_JSON
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_PDF
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.ExportCalendarJobContext
import com.bosch.pt.iot.smartsite.project.calendar.facade.rest.CalendarExportController.Companion.EXPORT_CSV_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.calendar.facade.rest.CalendarExportController.Companion.EXPORT_JSON_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.calendar.facade.rest.CalendarExportController.Companion.EXPORT_PDF_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.util.Locale.UK
import org.apache.avro.specific.SpecificRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SmartSiteMockKTest
@EnableAllKafkaListeners
class CalendarExportApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @Autowired private lateinit var jobJsonSerializer: JobJsonSerializer

  @MockkBean(name = "defaultCustomUserAuthenticationConverter")
  private lateinit var userAuthenticationConverter: DefaultCustomUserAuthenticationConverter

  private lateinit var token: UsernamePasswordAuthenticationToken

  private lateinit var project: Project

  @BeforeEach
  fun setUp() {
    eventStreamGenerator.setupDatasetTestData()
    val user = repositories.findUser(getIdentifier("user"))!!
    project = repositories.findProject(getIdentifier("project").asProjectId())!!

    // we need a jwt token here since the pdf endpoint passes the token on to the job service via
    // message otherwise the PDF converter service cannot authorize itself later at the export
    // callback
    token = UsernamePasswordAuthenticationToken(user, "n/a", user.authorities)
    every { userAuthenticationConverter.convert(any()) } returns token

    useOnlineListener()
    commandSendingService.clearRecords()
  }

  @Test
  fun `verify and document export calendar as PDF for project with given identifier`() {
    mockMvc
        .perform(
            requestBuilder(
                    post(latestVersionOf(EXPORT_PDF_BY_PROJECT_ID_ENDPOINT), project.identifier),
                    exportParameters())
                .header(AUTHORIZATION, BEARER_TOKEN))
        .andExpect(status().isAccepted)
        .andDo(
            document(
                "exports/document-calendar-export-pdf",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("projectId")
                        .description("ID of the project from where to export the calendar")),
                requestFields(CALENDAR_EXPORT_REQUEST_FIELD_DESCRIPTORS),
                responseFields(CALENDAR_EXPORT_RESPONSE_FIELD_DESCRIPTORS)))

    assertThat(commandSendingService.capturedRecords).hasSize(1)
    commandSendingService.capturedRecords.first().value.apply {
      assertJobTypeAndIdentifier(this, CALENDAR_EXPORT_PDF)
      assertContext(this)
      assertExportCalendarToPdfCommand(this)
    }
  }

  @Test
  fun `verify and document export calendar as JSON for project with given identifier`() {
    mockMvc
        .perform(
            requestBuilder(
                    post(latestVersionOf(EXPORT_JSON_BY_PROJECT_ID_ENDPOINT), project.identifier),
                    exportParameters())
                .header(AUTHORIZATION, BEARER_TOKEN))
        .andExpect(status().isAccepted)
        .andDo(
            document(
                "exports/document-calendar-export-json",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("projectId")
                        .description("ID of the project from where to export the calendar")),
                requestFields(CALENDAR_EXPORT_REQUEST_FIELD_DESCRIPTORS),
                responseFields(CALENDAR_EXPORT_RESPONSE_FIELD_DESCRIPTORS)))

    assertThat(commandSendingService.capturedRecords).hasSize(1)
    commandSendingService.capturedRecords.first().value.apply {
      assertJobTypeAndIdentifier(this, CALENDAR_EXPORT_JSON)
      assertContext(this)
      assertExportCalendarAsJsonCommand(this)
    }
  }

  @Test
  fun `verify and document export calendar as CSV for project with given identifier`() {
    mockMvc
        .perform(
            requestBuilder(
                    post(latestVersionOf(EXPORT_CSV_BY_PROJECT_ID_ENDPOINT), project.identifier),
                    exportParameters())
                .header(AUTHORIZATION, BEARER_TOKEN))
        .andExpect(status().isAccepted)
        .andDo(
            document(
                "exports/document-calendar-export-csv",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("projectId")
                        .description("ID of the project from where to export the calendar")),
                requestFields(CALENDAR_EXPORT_REQUEST_FIELD_DESCRIPTORS),
                responseFields(CALENDAR_EXPORT_RESPONSE_FIELD_DESCRIPTORS)))

    assertThat(commandSendingService.capturedRecords).hasSize(1)
    commandSendingService.capturedRecords.first().value.apply {
      assertJobTypeAndIdentifier(this, CALENDAR_EXPORT_CSV)
      assertContext(this)
      assertExportCalendarAsCsvCommand(this)
    }
  }

  private fun assertJobTypeAndIdentifier(message: SpecificRecord, jobType: CalendarExportJobType) {
    assertThat(message).isInstanceOf(EnqueueJobCommandAvro::class.java)
    (message as EnqueueJobCommandAvro).apply {
      assertThat(getJobType()).isEqualTo(jobType.name)
      assertThat(this.aggregateIdentifier.type).isEqualTo("JOB")
    }
  }

  private fun assertContext(message: SpecificRecord) {
    (message as EnqueueJobCommandAvro).jsonSerializedContext.apply {
      ExportCalendarJobContext(ResourceReference.from(project))
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun assertExportCalendarToPdfCommand(message: SpecificRecord) {
    (message as EnqueueJobCommandAvro).jsonSerializedCommand.apply {
      ExportCalendarAsPdfCommand(
              locale = UK,
              projectIdentifier = project.identifier,
              calendarExportParameters = exportParameters(),
              token = BEARER_TOKEN)
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun assertExportCalendarAsJsonCommand(message: SpecificRecord) {
    (message as EnqueueJobCommandAvro).jsonSerializedCommand.apply {
      ExportCalendarAsJsonCommand(
              locale = UK,
              projectIdentifier = project.identifier,
              calendarExportParameters = exportParameters())
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun assertExportCalendarAsCsvCommand(message: SpecificRecord) {
    (message as EnqueueJobCommandAvro).jsonSerializedCommand.apply {
      ExportCalendarAsCsvCommand(
              locale = UK,
              projectIdentifier = project.identifier,
              calendarExportParameters = exportParameters())
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun exportParameters() =
      CalendarExportParameters(
          AssigneesFilter(
              listOf(getIdentifier("participant").asParticipantId()),
              listOf(getIdentifier("company"))),
          project.start,
          project.end,
          emptyList(),
          emptyList(),
          listOf(*TaskStatusEnum.values()),
          listOf(*TopicCriticalityEnum.values()),
          hasTopics = false,
          includeDayCards = true,
          includeMilestones = true,
          allDaysInDateRange = false)

  companion object {
    private const val BEARER_TOKEN =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFt" +
            "ZSI6IkpvaG4gRG9lIiwiaXNzIjoiaHR0cHM6Ly9qd3RjcmVhdG9yLmV4YW1wbGUuY29tIiwiaWF0IjoxNT" +
            "E2MjM5MDIyfQ.prX7A3FUhK_m3tRjCj3fgLkA84_gaSikQ2m7q1XPtx4"

    private val CALENDAR_EXPORT_CONSTRAINED_FIELDS =
        ConstrainedFields(CalendarExportParameters::class.java)

    private val CALENDAR_EXPORT_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("from")
                .description("The initial date from where the calendar should be exported.")
                .type(STRING),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("to")
                .description("The final date to where the calendar should be exported.")
                .type(STRING),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("assignees")
                .optional()
                .description(
                    "A JSON object to filter tasks by their assignee (Optional). " +
                        "If participant ids and company ids are both set, those tasks are exported where either " +
                        "the assignee's participant id matches or the assignee's company id, or both.")
                .type(OBJECT),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("assignees.participantIds")
                .optional()
                .description(
                    "Participant Ids used to filter tasks of the exported calendar (Optional)")
                .type(ARRAY),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("assignees.companyIds")
                .optional()
                .description("Company Ids used to filter tasks of the exported calendar (Optional)")
                .type(ARRAY),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("projectCraftIds")
                .optional()
                .description("Craft Ids used to filter tasks of the exported calendar (Optional)")
                .type(ARRAY),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("workAreaIds")
                .optional()
                .description(
                    "Work area Ids used to filter tasks of the exported calendar (Optional)")
                .type(ARRAY),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("status")
                .optional()
                .description(
                    "Tasks states used to filter tasks of the exported calendar (Optional)")
                .attributes(
                    key("constraints")
                        .value("Valid values for the entries are DRAFT,OPEN,STARTED,CLOSED"))
                .type(ARRAY),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("topicCriticality")
                .optional()
                .description(
                    "Topic criticality status used to filter tasks only with topics " +
                        "of the exported calendar (Optional)")
                .attributes(
                    key("constraints")
                        .value("Valid values for the entries are CRITICAL, UNCRITICAL"))
                .type(ARRAY),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("hasTopics")
                .optional()
                .description(
                    "Boolean used to filter tasks only with topics of the exported calendar (Optional)")
                .type(BOOLEAN),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("includeDayCards")
                .optional()
                .description(
                    "Boolean used to request calendar with or without day cards (Optional, default false)")
                .type(BOOLEAN),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("includeMilestones")
                .optional()
                .description(
                    "Boolean used to request calendar with or without milestones (Optional, default false)")
                .type(BOOLEAN),
            CALENDAR_EXPORT_CONSTRAINED_FIELDS.withPath("allDaysInDateRange")
                .optional()
                .description(
                    "Boolean used to request calendar with tasks only in date range (Optional)")
                .type(BOOLEAN))

    private val CALENDAR_EXPORT_RESPONSE_FIELD_DESCRIPTORS =
        listOf(fieldWithPath("id").description("Unique identifier of the export Job").type(STRING))
  }
}
