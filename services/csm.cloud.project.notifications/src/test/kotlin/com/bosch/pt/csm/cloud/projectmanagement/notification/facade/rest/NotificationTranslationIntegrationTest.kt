/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCsm
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import java.util.Locale
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@DisplayName("Verify notification api")
@SmartSiteSpringBootTest
class NotificationTranslationIntegrationTest : AbstractNotificationApiTest() {

  @Autowired private lateinit var notificationService: NotificationService

  private lateinit var notification: Notification

  @BeforeEach
  override fun setup() {
    super.setup()
    this.mockMvc = webAppContextSetup(webApplicationContext).build()
    Locale.setDefault(Locale.ENGLISH)
    eventStreamGenerator.submitTaskAsFm()
    repositories.notificationRepository.deleteAll()

    // Setup test data
    eventStreamGenerator
        .submitTaskAsCsm()
        .submitDayCardG2(asReference = "dayCard")
        // Create notifications
        .submitDayCardG2(
            asReference = "dayCard",
            auditUserReference = FM_USER,
            eventType = DayCardEventEnumAvro.UPDATED) { it.notes = "update 1" }

    val notifications = notificationService.findAll(csmUser.identifier, 2)
    notification = notifications.resources!![0]

    // Delete user who initiated the notification
    repositories.userRepository.deleteUser(fmUser.identifier)

    LocaleContextHolder.setLocale(null)
  }

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = Locale.GERMANY

    eventStreamGenerator.submitUser(asReference = CSM_USER, eventType = UPDATED) {
      it.locale = expectedLocale.toString()
    }

    requestNotifications(notification, null)
        .andExpect(status().isOk)
        .andExpect(isExpectedNotification())
        .andExpect(isTranslatedDeletedUserMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = Locale.FRANCE

    eventStreamGenerator.submitUser(asReference = CSM_USER, eventType = UPDATED) {
      it.locale = Locale.GERMANY.toString()
    }

    requestNotifications(notification, expectedLocale)
        .andExpect(status().isOk)
        .andExpect(isExpectedNotification())
        .andExpect(isTranslatedDeletedUserMessage(expectedLocale))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(asReference = CSM_USER, eventType = UPDATED) {
      it.locale = null
    }

    requestNotifications(notification, null)
        .andExpect(status().isOk)
        .andExpect(isExpectedNotification())
        .andExpect(isTranslatedDeletedUserMessage(Locale.UK))
  }

  private fun requestNotifications(notification: Notification, locale: Locale?): ResultActions {
    return doWithAuthorization(
        repositories.userRepository.findOneCachedByIdentifier(csmUser.identifier)) {
      mockMvc.perform(
          get(latestVersionOf("/projects/notifications/{id}"), notification.externalIdentifier)
              .locale(locale)
              .header(ACCEPT, HAL_JSON_VALUE)
              .let { if (locale == null) it else it.header(ACCEPT_LANGUAGE, locale.toString()) })
    }
  }

  private fun isExpectedNotification(): ResultMatcher =
      jsonPath("$.id").value(notification.externalIdentifier.toString())

  private fun isTranslatedDeletedUserMessage(locale: Locale) =
      jsonPath("$.actor.displayName")
          .value(messageSource.getMessage(Key.USER_DELETED, null, locale))
}
