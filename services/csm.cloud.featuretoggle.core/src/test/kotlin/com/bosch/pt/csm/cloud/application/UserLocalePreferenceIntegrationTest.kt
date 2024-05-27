/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application

import com.bosch.pt.csm.cloud.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.translation.Key
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Verify api")
class UserLocalePreferenceIntegrationTest : AbstractApiDocumentationTest() {

  private lateinit var userIdentifier: UUID

  @BeforeEach
  fun init() {
    Locale.setDefault(ENGLISH)
    eventStreamGenerator.submitUserAndActivate("daniel") {}.submitUser("daniel") { it.admin = true }

    userIdentifier = get<UserAggregateAvro>(USER_DANIEL)!!.getIdentifier()
    setAuthentication("daniel")
  }

  private fun tryAndDeleteMissingFeature(): ResultActions =
      mockMvc.perform(
          requestBuilder(
              RestDocumentationRequestBuilders.delete(latestVersionOf("/features/projectImport"))))

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = GERMANY

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = expectedLocale.toString()
    }

    tryAndDeleteMissingFeature()
        .andExpectAll(status().isNotFound, isTranslatedErrorMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = FRANCE

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = GERMANY.toString()
    }

    tryAndDeleteMissingFeature().andExpect(isTranslatedErrorMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = null
    }

    tryAndDeleteMissingFeature()
        .andExpect(status().isNotFound)
        .andExpect(isTranslatedErrorMessage(Locale.UK))
  }

  private fun isTranslatedErrorMessage(locale: Locale) =
      jsonPath("$.message")
          .value(
              messageSource.getMessage(
                  Key.FEATURE_TOGGLE_VALIDATION_ERROR_FEATURE_NOT_FOUND, null, locale))

  companion object {
    private const val USER_DANIEL = "daniel"
  }
}
