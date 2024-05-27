/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCsm
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.test.RestUtils.requestBuilder
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import java.time.Instant.now
import java.util.Date
import java.util.Locale
import java.util.Locale.ENGLISH
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@DisplayName("Verify notification api")
@SmartSiteSpringBootTest
class NotificationApiTest : AbstractNotificationApiTest() {

  @Autowired lateinit var userService: UserService

  @Autowired lateinit var notificationService: NotificationService

  @BeforeEach
  override fun setup() {
    super.setup()
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    Locale.setDefault(ENGLISH)
    eventStreamGenerator.submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  @DisplayName("returns a 4XX if before and after parameters are specified together")
  fun verifyFindAllNotificationsForUserFails() {
    val before = now().plusMillis(10000)
    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf(
                          "/projects/notifications?before={before}&after={after}&limit={limit}"),
                      df.format(Date(before.toEpochMilli())),
                      df.format(Date(before.toEpochMilli())),
                      2),
                  objectMapper))
          .andExpect(status().is4xxClientError)
    }
  }

  @Test
  @DisplayName("returns notifications for user when 'after' parameter is specified")
  fun verifyFindAllNotificationsForUserAfterDate() {
    eventStreamGenerator
        .submitDayCardG2(asReference = "dayCard", auditUserReference = FM_USER)
        // Create notification before a point in time
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 1"
        }

    // Set after date for request
    val after = now()
    userService.setLastSeen(fmUser.identifier, Date(after.toEpochMilli()))

    // Create notification after a point in time
    eventStreamGenerator
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 2"
        }
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 3"
        }

    val notifications = notificationService.findAllAfter(fmUser.identifier, after, 2)
    val notificationFromUpdate3 = notifications.resources!![0]
    val notificationFromUpdate2 = notifications.resources!![1]

    // Mark the notifications from the second update as read
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate2.externalIdentifier!!)

    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf("/projects/notifications?after={after}&limit={limit}"),
                      df.format(Date(after.toEpochMilli())),
                      2),
                  objectMapper))
          .andExpect(status().isOk)
          .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(2))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.items[0].id")
                  .value(notificationFromUpdate3.externalIdentifier.toString()))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.items[1].id")
                  .value(notificationFromUpdate2.externalIdentifier.toString()))
    }
  }

  @Test
  @DisplayName("returns notifications for user")
  fun verifyFindAllNotificationsForUser() {
    eventStreamGenerator
        .submitTaskAsFm()
        .submitDayCardG2(asReference = "dayCard", auditUserReference = FM_USER)
        // Create notifications
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 1"
        }
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 2"
        }
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 3"
        }

    val notifications = notificationService.findAll(fmUser.identifier, 3)
    val notificationFromUpdate3 = notifications.resources!![0]
    val notificationFromUpdate2 = notifications.resources!![1]
    val notificationFromUpdate1 = notifications.resources!![2]

    // Mark the notifications from the first and second update as read
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate1.externalIdentifier!!)
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate2.externalIdentifier!!)

    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(latestVersionOf("/projects/notifications?limit={limit}"), 4), objectMapper))
          .andExpect(status().isOk)
          .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(3))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.items[0].id")
                  .value(notificationFromUpdate3.externalIdentifier.toString()))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.items[1].id")
                  .value(notificationFromUpdate2.externalIdentifier.toString()))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.items[2].id")
                  .value(notificationFromUpdate1.externalIdentifier.toString()))
    }
  }

  @Test
  @DisplayName("returns single notification")
  fun verifyFindSingleNotification() {
    eventStreamGenerator
        .submitTaskAsFm()
        .submitDayCardG2(asReference = "dayCard", auditUserReference = FM_USER)
        // Create notifications
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 1"
        }
        .submitDayCardG2(asReference = "dayCard", eventType = DayCardEventEnumAvro.UPDATED) {
          it.notes = "update 2"
        }

    val notifications = notificationService.findAll(fmUser.identifier, 2)
    val notificationFromUpdate1 = notifications.resources!![1]

    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf("/projects/notifications/{id}"),
                      notificationFromUpdate1.externalIdentifier.toString()),
                  objectMapper))
          .andExpect(status().isOk)
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.id")
                  .value(notificationFromUpdate1.externalIdentifier.toString()))
    }
  }

  @Test
  @DisplayName("returns notification with reference to deleted user")
  fun verifyNotificationReferenceToDeletedUser() {
    eventStreamGenerator
        .submitTaskAsCsm()
        .submitDayCardG2(asReference = "dayCard")
        // Create notifications
        .submitDayCardG2(
            asReference = "dayCard",
            auditUserReference = FM_USER,
            eventType = DayCardEventEnumAvro.UPDATED) { it.notes = "update 1" }

    val notifications = notificationService.findAll(csmUser.identifier, 2)
    val notificationFromUpdate = notifications.resources!![0]

    // Delete user who initiated the notification
    repositories.userRepository.deleteUser(fmUser.identifier)

    // Check that [Deleted User] is returned as display name for the deleted initiator
    doWithAuthorization(csmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf("/projects/notifications/{id}"),
                      notificationFromUpdate.externalIdentifier.toString()),
                  objectMapper))
          .andExpect(status().isOk)
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.id")
                  .value(notificationFromUpdate.externalIdentifier.toString()))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.summary.values.originator.text")
                  .value(
                      messageSource.getMessage(
                          Key.USER_DELETED, null, LocaleContextHolder.getLocale())))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.actor.displayName")
                  .value(
                      messageSource.getMessage(
                          Key.USER_DELETED, null, LocaleContextHolder.getLocale())))
    }
  }
}
