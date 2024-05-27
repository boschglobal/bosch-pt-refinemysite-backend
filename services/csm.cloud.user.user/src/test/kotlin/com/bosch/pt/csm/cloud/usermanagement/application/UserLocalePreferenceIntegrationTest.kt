/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.CRAFT_BY_CRAFT_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import java.util.Locale
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Verify api")
class UserLocalePreferenceIntegrationTest : AbstractApiDocumentationTest() {

  private lateinit var userIdentifier: UUID

  private lateinit var craftIdentifier: UUID

  @BeforeEach
  fun init() {
    Locale.setDefault(Locale.ENGLISH)

    eventStreamGenerator
        .submitUserAndActivate(USER_DANIEL) { it.admin = true }
        .submitCraft("electricity") {
          it.defaultName = "Electricity"
          it.translations =
              listOf(
                  CraftTranslationAvro(Locale.GERMAN.toString(), "Elektrizität"),
                  CraftTranslationAvro(Locale.FRENCH.toString(), "Électricité"),
                  CraftTranslationAvro(Locale.ENGLISH.toString(), "Electricity"))
        }

    userIdentifier = get<UserAggregateAvro>(USER_DANIEL)!!.getIdentifier()
    craftIdentifier = get<CraftAggregateAvro>("electricity")!!.getIdentifier()
  }

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = Locale.GERMANY

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = expectedLocale.toString()
    }

    setAuthentication(USER_DANIEL)
    requestCrafts(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedCraft())
        .andExpect(isTranslatedCraft(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = Locale.FRANCE

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = Locale.GERMANY.toString()
    }

    setAuthentication(USER_DANIEL)
    requestCrafts(expectedLocale)
        .andExpect(status().isOk)
        .andExpect(isExpectedCraft())
        .andExpect(isTranslatedCraft(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = null
    }

    setAuthentication(USER_DANIEL)
    requestCrafts(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedCraft())
        .andExpect(isTranslatedCraft(Locale.UK))
  }

  @Test
  fun `returns messages in the correct language with default locale for unregistered user`() {
    TestSecurityContextHolder.setAuthentication(
        UsernamePasswordAuthenticationToken(
            UnregisteredUser("test", "test@example.com"), "n/a", createAuthorityList("ROLE_USER")))

    requestCrafts(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedCraft())
        .andExpect(isTranslatedCraft(Locale.UK))
  }

  private fun requestCrafts(locale: Locale?): ResultActions {
    return mockMvc.perform(
        get(latestVersionOfCraftApi(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH), craftIdentifier)
            .locale(locale)
            .accept(HAL_JSON_VALUE))
  }

  private fun isExpectedCraft(): ResultMatcher = jsonPath("$.id").value(craftIdentifier.toString())

  private fun isTranslatedCraft(locale: Locale) =
      jsonPath("$.name")
          .value(
              when (locale) {
                Locale.UK -> "Electricity"
                Locale.FRANCE -> "Électricité"
                Locale.GERMANY -> "Elektrizität"
                else -> throw IllegalArgumentException("Unexpected locale: $locale")
              })

  companion object {
    private const val USER_DANIEL = "daniel"
  }
}
