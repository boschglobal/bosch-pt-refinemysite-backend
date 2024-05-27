/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.IF_MATCH_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.PROJECT_REFERENCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasReference
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.WorkdayConfigurationController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.HolidayResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.request.UpdateWorkdayConfigurationResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.WorkdayConfigurationResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.workday.util.WorkdayConfigurationTestUtils.verifyUpdatedAggregate
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.ARRAY
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class WorkdayConfigurationApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val workdayConfiguration by lazy {
    repositories.findWorkdayConfiguration(
        getIdentifier("workdayConfiguration").asWorkdayConfigurationId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document find one workday configuration by project identifier`() {
    val sortedWorkdays = workdayConfiguration.workingDays.sorted()

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/{projectId}/workdays"), project.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"0\""),
            *hasIdentifierAndVersion(workdayConfiguration.identifier.toUuid()),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(creatorUser),
            *hasReference(project),
            jsonPath("$.startOfWeek").value(workdayConfiguration.startOfWeek.name),
            jsonPath("$.workingDays.length()").value(workdayConfiguration.workingDays.size),
            jsonPath("$.workingDays[0]").value(sortedWorkdays[0].name),
            jsonPath("$.workingDays[1]").value(sortedWorkdays[1].name),
            jsonPath("$.workingDays[2]").value(sortedWorkdays[2].name),
            jsonPath("$.workingDays[3]").value(sortedWorkdays[3].name),
            jsonPath("$.workingDays[4]").value(sortedWorkdays[4].name),
            jsonPath("$.holidays.length()").value(workdayConfiguration.holidays.size),
            jsonPath("$.holidays[0].name").value(workdayConfiguration.holidays.first().name),
            jsonPath("$.holidays[0].date")
                .value(workdayConfiguration.holidays.first().date.toString()),
            jsonPath("$.allowWorkOnNonWorkingDays").value(true))
        .andDo(
            document(
                "workday-configuration/document-get-workday-configuration",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_ID_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(WORKDAY_CONFIGURATION_RESPONSE_FIELD_DESCRIPTORS),
                links(WORKDAY_CONFIGURATION_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update workday configuration`() {
    val resource =
        UpdateWorkdayConfigurationResource(
            startOfWeek = FRIDAY,
            workingDays = listOf(FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY),
            holidays =
                listOf(
                    HolidayResource("Holiday_1", now().plusDays(1)),
                    HolidayResource("Holiday_2", now().plusDays(2))),
            false)

    val sortedWorkdays = resource.workingDays.sorted()
    val sortedHolidays = resource.holidays.sortedWith(compareBy({ it.date }, { it.name }))

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/{projectId}/workdays"), project.identifier),
                resource,
                0L))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *hasIdentifierAndVersion(workdayConfiguration.identifier.toUuid(), 1),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(testUser),
            *hasReference(project),
            jsonPath("$.startOfWeek").value(resource.startOfWeek.name),
            jsonPath("$.workingDays.length()").value(resource.workingDays.size),
            jsonPath("$.workingDays[0]").value(sortedWorkdays[0].name),
            jsonPath("$.workingDays[1]").value(sortedWorkdays[1].name),
            jsonPath("$.workingDays[2]").value(sortedWorkdays[2].name),
            jsonPath("$.workingDays[3]").value(sortedWorkdays[3].name),
            jsonPath("$.workingDays[4]").value(sortedWorkdays[4].name),
            jsonPath("$.holidays.length()").value(resource.holidays.size),
            jsonPath("$.holidays[0].name").value(sortedHolidays[0].name),
            jsonPath("$.holidays[0].date").value(sortedHolidays[0].date.toString()),
            jsonPath("$.holidays[1].name").value(sortedHolidays[1].name),
            jsonPath("$.holidays[1].date").value(sortedHolidays[1].date.toString()),
            jsonPath("$.allowWorkOnNonWorkingDays").value(false))
        .andDo(
            document(
                "workday-configuration/document-update-workday-configuration",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(PROJECT_ID_PATH_PARAMETER_DESCRIPTOR),
                requestFields(WORKDAY_CONFIGURATION_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(WORKDAY_CONFIGURATION_RESPONSE_FIELD_DESCRIPTORS),
                links(WORKDAY_CONFIGURATION_LINK_DESCRIPTORS)))

    val updateWorkDayConfiguration =
        repositories.workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(
            (project.identifier))!!
    projectEventStoreUtils
        .verifyContainsAndGet(WorkdayConfigurationEventAvro::class.java, UPDATED, 1, true)
        .also { verifyUpdatedAggregate(it[0].aggregate, updateWorkDayConfiguration) }
  }

  companion object {

    private val PROJECT_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")

    private val WORKDAY_CONFIGURATION_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(UpdateWorkdayConfigurationResource::class.java)
                .withPath("startOfWeek")
                .description("The start day of the week")
                .type(STRING),
            ConstrainedFields(UpdateWorkdayConfigurationResource::class.java)
                .withPath("workingDays")
                .description("The working days")
                .type(ARRAY),
            ConstrainedFields(UpdateWorkdayConfigurationResource::class.java)
                .withPath("holidays")
                .description("The holidays days")
                .type(ARRAY),
            ConstrainedFields(UpdateWorkdayConfigurationResource::class.java)
                .withPath("holidays[]", "name")
                .description("The name of the holiday")
                .attributes(key("constraints").value("Size must be between 1 and 100 inclusive"))
                .type(STRING),
            ConstrainedFields(UpdateWorkdayConfigurationResource::class.java)
                .withPath("holidays[]", "date")
                .description("The date of the holiday")
                .type(STRING),
            ConstrainedFields(UpdateWorkdayConfigurationResource::class.java)
                .withPath("allowWorkOnNonWorkingDays")
                .description(
                    "Toggle that specify if it is allowed to have day cards on non working days")
                .type(BOOLEAN))

    private val WORKDAY_CONFIGURATION_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            *PROJECT_REFERENCE_FIELD_DESCRIPTORS,
            fieldWithPath("startOfWeek").description("The start day of the week").type(STRING),
            fieldWithPath("workingDays")
                .description("The working days sorted by week day")
                .type(ARRAY),
            fieldWithPath("holidays")
                .description("The holidays days sorted by date and name")
                .type(ARRAY),
            fieldWithPath("holidays[].name").description("The name of the holiday").type(STRING),
            fieldWithPath("holidays[].date").description("The date of the holiday").type(STRING),
            fieldWithPath("allowWorkOnNonWorkingDays")
                .description(
                    "Toggle that specify if it is allowed to have day cards on non working days")
                .type(BOOLEAN),
            subsectionWithPath("_links").ignored())

    private val WORKDAY_CONFIGURATION_LINK_DESCRIPTORS =
        listOf(linkWithRel(LINK_UPDATE).description("Link to the resource to update"))
  }
}
