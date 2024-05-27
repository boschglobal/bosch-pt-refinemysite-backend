/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteApiDocumentationTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.UserDetailsServiceImpl
import com.bosch.pt.csm.cloud.projectmanagement.application.security.authorizeWithUser
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.NamedObjectService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.ParticipantMappingService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.RfvCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.StatisticsService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.UserService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.authorization.StatisticsAuthorizationComponent
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController.Companion.PPC_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController.Companion.PROJECT_METRICS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController.Companion.RFV_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum.BAD_WEATHER
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum.CONCESSION_NOT_RECOGNIZED
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum.DELAYED_MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum.DONE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.NamedObject
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountGroupedEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountGroupedEntry
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.LocalDate
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.UUID
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.MessageSource
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SmartSiteApiDocumentationTest
class StatisticsApiDocumentationTest {

  private val statisticsListResponseFields =
      responseFields(
          fieldWithPath("items[]").description("List of statistics"),
          fieldWithPath("items[].start").description("The start date of the duration"),
          fieldWithPath("items[].end").description("The end date of the duration"),
          fieldWithPath("items[].totals")
              .description("The total calculated metrics from start to end date.")
              .type(OBJECT),
          fieldWithPath("items[].totals.ppc")
              .description("The total calculated ppc from start to end date.")
              .optional(),
          fieldWithPath("items[].totals.rfv")
              .description("The total calculated rfv from start to end date.")
              .type(ARRAY),
          fieldWithPath("items[].totals.rfv[].reason")
              .description("The reason why day cards are not done.")
              .type(OBJECT),
          fieldWithPath("items[].totals.rfv[].reason.key")
              .description("The technical key of the reason.")
              .type(STRING),
          fieldWithPath("items[].totals.rfv[].reason.name")
              .description("The display name of the reason.")
              .type(STRING),
          fieldWithPath("items[].totals.rfv[].value")
              .description("The count of day cards of the given reason.")
              .type(NUMBER),
          fieldWithPath("items[].series[]")
              .description(
                  "The list of statistics entries for each week between the given start and end date."),
          fieldWithPath("items[].series[].start").description("The start date of the week."),
          fieldWithPath("items[].series[].end").description("The end date of the week."),
          fieldWithPath("items[].series[].metrics")
              .description("The metrics calculated for the week.")
              .type(OBJECT),
          fieldWithPath("items[].series[].metrics.ppc")
              .description("The ppc calculated for the week.")
              .optional(),
          fieldWithPath("items[].series[].metrics.rfv")
              .description("The total rfv calculated for the week.")
              .type(ARRAY)
              .optional(),
          fieldWithPath("items[].series[].metrics.rfv[].reason")
              .description("The reason why day cards are not done.")
              .type(OBJECT),
          fieldWithPath("items[].series[].metrics.rfv[].reason.key")
              .description("The technical key of the reason.")
              .type(STRING),
          fieldWithPath("items[].series[].metrics.rfv[].reason.name")
              .description("The display name of the reason.")
              .type(STRING),
          fieldWithPath("items[].series[].metrics.rfv[].value")
              .description("The count of day cards of the given reason.")
              .type(NUMBER),
          subsectionWithPath("_links").ignored())

  private val statisticsListResponseFieldsGrouped =
      responseFields(
          fieldWithPath("items[]").description("List of statistics grouped by company and craft"),
          fieldWithPath("items[].company.id")
              .description("The company identifier")
              .type(STRING)
              .optional(),
          fieldWithPath("items[].company.displayName")
              .description("The company name")
              .type(STRING)
              .optional(),
          fieldWithPath("items[].projectCraft.id")
              .description("The project craft identifier")
              .type(STRING)
              .optional(),
          fieldWithPath("items[].projectCraft.displayName")
              .description("The project craft name")
              .type(STRING)
              .optional(),
          fieldWithPath("items[].start").description("The start date of the duration"),
          fieldWithPath("items[].end").description("The end date of the duration"),
          fieldWithPath("items[].totals")
              .description("The total metrics calculated from start to end date.")
              .type(OBJECT),
          fieldWithPath("items[].totals.ppc")
              .description("The total ppc calculated from start to end date.")
              .optional(),
          fieldWithPath("items[].totals.rfv")
              .description("The total rfv calculated from start to end date.")
              .type(ARRAY),
          fieldWithPath("items[].totals.rfv[].reason")
              .description("The reason why day cards are not done.")
              .type(OBJECT),
          fieldWithPath("items[].totals.rfv[].reason.key")
              .description("The technical key of the reason.")
              .type(STRING),
          fieldWithPath("items[].totals.rfv[].reason.name")
              .description("The display name of the reason.")
              .type(STRING),
          fieldWithPath("items[].totals.rfv[].value")
              .description("The count of day cards of the given reason.")
              .type(NUMBER),
          fieldWithPath("items[].series[].metrics.rfv")
              .description("The total rfv calculated for the week.")
              .type(ARRAY)
              .optional(),
          fieldWithPath("items[].totals.rfv[].reason")
              .description("The reason why day cards are not done.")
              .type(OBJECT),
          fieldWithPath("items[].totals.rfv[].reason.key")
              .description("The technical key of the reason.")
              .type(STRING),
          fieldWithPath("items[].totals.rfv[].reason.name")
              .description("The display name of the reason.")
              .type(STRING),
          fieldWithPath("items[].totals.rfv[].value")
              .description("The count of day cards of the given reason.")
              .type(NUMBER),
          fieldWithPath("items[].series[].metrics.rfv[].reason")
              .description("The reason why day cards are not done.")
              .type(OBJECT),
          fieldWithPath("items[].series[].metrics.rfv[].reason.key")
              .description("The technical key of the reason.")
              .type(STRING),
          fieldWithPath("items[].series[].metrics.rfv[].reason.name")
              .description("The display name of the reason.")
              .type(STRING),
          fieldWithPath("items[].series[].metrics.rfv[].value")
              .description("The count of day cards of the given reason.")
              .type(NUMBER),
          fieldWithPath("items[].series[]")
              .description(
                  "The list of statistics entries for each week between the given start and end date."),
          fieldWithPath("items[].series[].start").description("The start date of the week."),
          fieldWithPath("items[].series[].end").description("The end date of the week."),
          fieldWithPath("items[].series[].metrics")
              .description("The metrics calculated for the week.")
              .type(OBJECT),
          fieldWithPath("items[].series[].metrics.ppc")
              .description("The ppc calculated for the week.")
              .optional(),
          subsectionWithPath("_links").ignored())

  private val requestParametersSnippet =
      queryParameters(
          parameterWithName("startDate")
              .description("The start date from where the statistics are calculated for."),
          parameterWithName("duration")
              .description("The number of weeks the statistics are calculated for."),
          parameterWithName("type")
              .description(
                  "The list of types of metrics to be calculated. Supported types are 'ppc' and 'rfv'"),
          parameterWithName("grouped")
              .description(
                  "Parameter must be set to 'true' to get statistics results grouped by company and craft")
              .optional())

  @MockkBean(relaxed = true) private lateinit var statisticsService: StatisticsService

  @MockkBean(relaxed = true) private lateinit var namedObjectService: NamedObjectService

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var statisticsAuthorizationComponent: StatisticsAuthorizationComponent

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var participantMappingService: ParticipantMappingService

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var userDetailsService: UserDetailsServiceImpl

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var userService: UserService

  @MockkBean(relaxed = true) private lateinit var rfvCustomizationService: RfvCustomizationService

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var oAuth2ResourceServerProperties: OAuth2ResourceServerProperties

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var customUserAuthenticationConverter: DefaultCustomUserAuthenticationConverter

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var jwtDecoder: JwtDecoder

  @Autowired private lateinit var apiVersionProperties: ApiVersionProperties

  @Autowired private lateinit var mockMvc: MockMvc

  @Autowired private lateinit var messageSource: MessageSource

  @BeforeEach
  fun init() {
    every { rfvCustomizationService.resolveProjectRfvs(any()) } answers
        {
          DayCardReasonVarianceEnum.values().associateWith {
            messageSource.getMessage("DayCardReasonVarianceEnum_" + it.name, null, ENGLISH)
          }
        }
  }

  @Test
  fun verifyAndDocumentGetMetricsForProjectGrouped() {
    authorizeWithUser(User(randomUUID().toString(), randomUUID(), false, false, Locale.UK), true)

    val numWeeks = 6L
    val date = LocalDate.now()
    val projectIdentifier = randomUUID()
    val companyIdentifier = randomUUID()
    val craftIdentifier = randomUUID()
    val companyName = "Company A"
    val craftName = "Craft A"
    val dayCardCountGroupedEntries =
        getDayCardCountGroupedEntries(companyIdentifier, craftIdentifier)
    val dayCardReasonCountGroupedEntries =
        getDayCardReasonCountGroupedEntries(companyIdentifier, craftIdentifier)

    every {
      statisticsService.calculatePpcByCompanyAndCraft(projectIdentifier, date, numWeeks)
    } returns dayCardCountGroupedEntries
    every {
      statisticsService.calculateRfvByCompanyAndCraft(projectIdentifier, date, numWeeks)
    } returns dayCardReasonCountGroupedEntries
    every { statisticsService.calculateRfv(projectIdentifier, date, numWeeks) } returns
        generateDayCardReasonCountForWeeks()
    every { statisticsService.determineEndDate(date, numWeeks) } returns
        date.plusWeeks(numWeeks).minusDays(1)
    every { namedObjectService.findCompanyNames(any()) } returns
        setOf(NamedObject(AggregateType.COMPANY, companyIdentifier, companyName))
    every { namedObjectService.findProjectCraftNames(any()) } returns
        setOf(NamedObject(AggregateType.PROJECTCRAFT, craftIdentifier, craftName))

    mockMvc
        .perform(
            get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
                .param("startDate", date.toString())
                .param("duration", numWeeks.toString())
                .param("type", PPC_TYPE, RFV_TYPE)
                .param("grouped", "true")
                .locale(ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, ENGLISH.language))
        .andExpect(status().isOk)
        .andDo(
            document(
                "project-statistics/document-get-project-statistics-grouped",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("projectId")
                        .description("ID of the project the statistics are calculated for.")),
                requestParametersSnippet,
                statisticsListResponseFieldsGrouped))
  }

  @Test
  fun verifyAndDocumentGetMetricsForProject() {
    val numWeeks = 6L
    val date = LocalDate.now()
    val projectIdentifier = randomUUID()

    every { statisticsService.calculatePpc(projectIdentifier, date, numWeeks) } returns
        generateDayCardCountForWeeks()
    every {
      statisticsService.calculatePpcByCompanyAndCraft(projectIdentifier, date, numWeeks)
    } returns emptyList()
    every { statisticsService.calculateRfv(projectIdentifier, date, numWeeks) } returns
        generateDayCardReasonCountForWeeks()
    every { statisticsService.determineEndDate(date, numWeeks) } returns
        date.plusWeeks(numWeeks).minusDays(1)

    mockMvc
        .perform(
            get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
                .param("startDate", date.toString())
                .param("duration", numWeeks.toString())
                .param("type", java.lang.String.join(",", PPC_TYPE, RFV_TYPE))
                .param("grouped", "false")
                .locale(ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, ENGLISH.language))
        .andExpect(status().isOk)
        .andDo(
            document(
                "project-statistics/document-get-project-statistics",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("projectId")
                        .description("ID of the project the statistics are calculated for.")),
                requestParametersSnippet,
                statisticsListResponseFields))
  }

  @Test
  fun verifyGetMetricsForProjectWithDurationLessThanOne() {
    val date = LocalDate.now()
    val numWeeks = 0L
    val projectIdentifier = randomUUID()

    every { statisticsService.determineEndDate(date, numWeeks) } returns
        date.plusWeeks(numWeeks).minusDays(1)

    mockMvc
        .perform(
            get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
                .param("startDate", date.toString())
                .param("duration", numWeeks.toString())
                .param("type", RFV_TYPE, PPC_TYPE)
                .locale(ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, ENGLISH.language))
        .andExpect(status().isBadRequest)
  }

  @Test
  fun verifyGetMetricsForProjectWithoutType() {
    val date = LocalDate.now()
    val numWeeks = 6L
    val projectIdentifier = randomUUID()

    mockMvc
        .perform(
            get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
                .param("startDate", date.toString())
                .param("duration", numWeeks.toString())
                .locale(ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, ENGLISH.language))
        .andExpect(status().isBadRequest)
  }

  @Test
  fun verifyGetMetricsForProjectFailsWithoutStartDate() {
    val numWeeks = 6L
    val projectIdentifier = randomUUID()

    mockMvc
        .perform(
            get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
                .param("duration", numWeeks.toString())
                .locale(ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, ENGLISH.language))
        .andExpect(status().isBadRequest)
  }

  @Test
  fun verifyGetMetricsForProjectFailsWithoutDuration() {
    val date = LocalDate.now()
    val projectIdentifier = randomUUID()

    mockMvc
        .perform(
            get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
                .param("startDate", date.toString())
                .locale(ENGLISH)
                .header(ACCEPT, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE, ENGLISH.language))
        .andExpect(status().isBadRequest)
  }

  private fun generateDayCardCountForWeeks(): List<DayCardCountEntry> =
      listOf(
          DayCardCountEntry(DayCardStatusEnum.OPEN, 5, 1),
          DayCardCountEntry(DayCardStatusEnum.NOTDONE, 3, 1),
          DayCardCountEntry(DONE, 5, 1),
          DayCardCountEntry(APPROVED, 7, 1),
          DayCardCountEntry(DayCardStatusEnum.OPEN, 2, 2),
          DayCardCountEntry(DayCardStatusEnum.NOTDONE, 1, 2))

  private fun generateDayCardReasonCountForWeeks(): List<DayCardReasonCountEntry> =
      listOf(
          DayCardReasonCountEntry(DELAYED_MATERIAL, 3, 1),
          DayCardReasonCountEntry(BAD_WEATHER, 5, 1),
          DayCardReasonCountEntry(CONCESSION_NOT_RECOGNIZED, 2, 2),
          DayCardReasonCountEntry(BAD_WEATHER, 1, 2))

  private fun getDayCardCountGroupedEntries(
      companyIdentifier: UUID,
      craftIdentifier: UUID
  ): List<DayCardCountGroupedEntry> =
      listOf(
          DayCardCountGroupedEntry(DONE, 4, 1, companyIdentifier, craftIdentifier),
          DayCardCountGroupedEntry(APPROVED, 3, 1, companyIdentifier, craftIdentifier))

  private fun getDayCardReasonCountGroupedEntries(companyIdentifier: UUID, craftIdentifier: UUID) =
      listOf(
          DayCardReasonCountGroupedEntry(BAD_WEATHER, 4, 1, companyIdentifier, craftIdentifier),
          DayCardReasonCountGroupedEntry(
              DELAYED_MATERIAL, 3, 1, companyIdentifier, craftIdentifier))

  private fun latestVersionOf(path: String): String = "/v${apiVersionProperties.version.max}$path"
}
