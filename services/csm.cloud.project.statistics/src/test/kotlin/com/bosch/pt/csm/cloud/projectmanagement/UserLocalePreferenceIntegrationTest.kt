/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController.Companion.PPC_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController.Companion.PROJECT_METRICS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController.Companion.RFV_TYPE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Verify api")
@SmartSiteSpringBootTest
class UserLocalePreferenceIntegrationTest : AbstractApiDocumentationTest() {

  private val startDate = LocalDate.now().minusDays(10)

  private lateinit var projectIdentifier: UUID

  @BeforeEach
  fun init() {
    Locale.setDefault(Locale.UK)

    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUserLocalePreferenceIntegrationTestData()

    projectIdentifier = getIdentifier("project")
  }

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = Locale.GERMANY

    eventStreamGenerator.submitUser(asReference = USER_CSM, eventType = UserEventEnumAvro.UPDATED) {
      it.locale = expectedLocale.toString()
    }

    requestStatistics(null)
        .andExpect(status().isOk)
        .andExpect(hasExpectedDate())
        .andExpect(isTranslatedReasonMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = Locale.FRANCE

    eventStreamGenerator.submitUser(asReference = USER_CSM, eventType = UserEventEnumAvro.UPDATED) {
      it.locale = Locale.GERMANY.toString()
    }

    requestStatistics(expectedLocale)
        .andExpect(status().isOk)
        .andExpect(hasExpectedDate())
        .andExpect(isTranslatedReasonMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(asReference = USER_CSM, eventType = UserEventEnumAvro.UPDATED) {
      it.locale = null
    }

    requestStatistics(null)
        .andExpect(status().isOk)
        .andExpect(hasExpectedDate())
        .andExpect(isTranslatedReasonMessage(Locale.UK))
  }

  private fun requestStatistics(locale: Locale?): ResultActions {
    return doWithAuthorization(
        repositories.userRepository.findOneByIdentifier(getIdentifier(USER_CSM))) {
      mockMvc.perform(
          get(latestVersionOf(PROJECT_METRICS_ENDPOINT), projectIdentifier)
              .locale(locale)
              .param("startDate", startDate.toString())
              .param("duration", "1")
              .param("type", PPC_TYPE, RFV_TYPE)
              .param("grouped", "false")
              .header(ACCEPT, HAL_JSON_VALUE)
              .let { if (locale == null) it else it.header(ACCEPT_LANGUAGE, locale.toString()) })
    }
  }

  private fun hasExpectedDate(): ResultMatcher =
      jsonPath("$.items[0].start").value(startDate.toString())

  private fun isTranslatedReasonMessage(locale: Locale) =
      jsonPath("$.items[0].totals.rfv[0].reason.name")
          .value(
              messageSource.getMessage("DayCardReasonVarianceEnum_DELAYED_MATERIAL", null, locale))

  companion object {
    private const val USER_CSM = "csmUser"
  }
}
