/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key
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

@EnableAllKafkaListeners
@DisplayName("Verify api")
class UserLocalePreferenceIntegrationTest : AbstractApiDocumentationTestV2() {

  private lateinit var projectIdentifier: UUID

  @BeforeEach
  fun init() {
    Locale.setDefault(Locale.ENGLISH)

    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUserAndActivate("bob")
        .submitCompany()
        .submitEmployee { it.roles = listOf(CSM) }
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(asReference = "csmParticipant")
        .submitUserAndActivate(USER_DANIEL) { it.admin = true }
        .submitEmployee("employeeDaniel") { it.roles = listOf(CSM) }
        .submitParticipantG3("csmDaniel")
        .submitUserTombstones("bob")

    projectIdentifier = get<ProjectAggregateAvro>("project")!!.getIdentifier()

    eventStreamGenerator.submitUserTombstones("bob")
  }

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = Locale.GERMANY

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = expectedLocale.toString()
    }

    requestProject(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedProject())
        .andExpect(isTranslatedDeletedUserMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = Locale.FRANCE

    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = Locale.GERMANY.toString()
    }

    requestProject(expectedLocale)
        .andExpect(status().isOk)
        .andExpect(isExpectedProject())
        .andExpect(isTranslatedDeletedUserMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(asReference = USER_DANIEL, eventType = UPDATED) {
      it.locale = null
    }

    requestProject(null)
        .andExpect(status().isOk)
        .andExpect(isExpectedProject())
        .andExpect(isTranslatedDeletedUserMessage(Locale.UK))
  }

  private fun requestProject(locale: Locale?): ResultActions {
    // Set authentication here to use the latest version of the user from database
    setAuthentication(USER_DANIEL)

    return mockMvc.perform(
        get(latestVersionOf("/projects/{projectId}"), projectIdentifier)
            .locale(locale)
            .header(ACCEPT, HAL_JSON_VALUE)
            .let { if (locale == null) it else it.header(ACCEPT_LANGUAGE, locale.toString()) })
  }

  private fun isExpectedProject(): ResultMatcher =
      jsonPath("$.id").value(projectIdentifier.toString())

  private fun isTranslatedDeletedUserMessage(locale: Locale) =
      jsonPath("$.createdBy.displayName")
          .value(messageSource.getMessage(Key.USER_DELETED, null, locale))

  companion object {
    private const val USER_DANIEL = "daniel"
  }
}
