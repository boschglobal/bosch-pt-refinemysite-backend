/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.application

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.common.i18n.Key
import com.bosch.pt.csm.company.company.asCompanyId
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
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
class UserLocalePreferenceIntegrationTest : AbstractApiDocumentationTest() {

  private val userIdentifier by lazy { getIdentifier(USER_DANIEL).asUserId() }

  private val companyIdentifier by lazy { getIdentifier("company").asCompanyId() }

  @BeforeEach
  fun init() {
    Locale.setDefault(ENGLISH)

    eventStreamGenerator
        .submitUserAndActivate("bob")
        .submitCompany()
        .submitEmployee { it.roles = listOf(CSM) }
        .submitUserAndActivate(USER_DANIEL) { it.admin = true }
        .submitEmployee("employeeDaniel") { it.roles = listOf(CSM) }

    eventStreamGenerator.submitUserTombstones("bob")
  }

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = GERMANY

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = expectedLocale.toString()
    }

    requestCompanies(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedCompany())
        .andExpect(isTranslatedDeletedUserMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = FRANCE

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = GERMANY.toString()
    }

    requestCompanies(expectedLocale)
        .andExpect(status().isOk)
        .andExpect(isExpectedCompany())
        .andExpect(isTranslatedDeletedUserMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = null
    }

    requestCompanies(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedCompany())
        .andExpect(isTranslatedDeletedUserMessage(Locale.UK))
  }

  private fun requestCompanies(locale: Locale?): ResultActions =
      doWithAuthorization(repositories.userProjectionRepository.findOneById(userIdentifier)!!) {
        mockMvc.perform(
            get(latestVersionOf("/companies/{companyId}"), companyIdentifier)
                .locale(locale)
                .header(ACCEPT, HAL_JSON_VALUE)
                .let { if (locale == null) it else it.header(ACCEPT_LANGUAGE, locale.toString()) })
      }

  private fun isExpectedCompany(): ResultMatcher =
      jsonPath("$.id").value(companyIdentifier.toString())

  private fun isTranslatedDeletedUserMessage(locale: Locale) =
      jsonPath("$.createdBy.displayName")
          .value(messageSource.getMessage(Key.USER_DELETED, null, locale))

  companion object {
    private const val USER_DANIEL = "daniel"
  }
}
