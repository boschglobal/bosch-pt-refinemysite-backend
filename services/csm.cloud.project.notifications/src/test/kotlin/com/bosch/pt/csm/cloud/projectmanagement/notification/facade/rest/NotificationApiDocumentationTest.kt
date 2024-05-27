/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.NotificationController.Companion.MARK_NOTIFICATION_AS_READ_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.NotificationController.Companion.NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.NotificationController.Companion.NOTIFICATION_LAST_SEEN_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.UpdateLastSeenResource
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.test.ConstrainedFields
import com.bosch.pt.csm.cloud.projectmanagement.test.RestUtils.requestBuilder
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@DisplayName("Verify notification api can")
@SmartSiteSpringBootTest
class NotificationApiDocumentationTest : AbstractNotificationApiTest() {

  @Autowired lateinit var userService: UserService
  @Autowired lateinit var notificationService: NotificationService

  @BeforeEach
  fun setup(restDocumentationContextProvider: RestDocumentationContextProvider) {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(
                documentationConfiguration(restDocumentationContextProvider)
                    .operationPreprocessors()
                    .withRequestDefaults(prettyPrint())
                    .withResponseDefaults(prettyPrint()))
            .build()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  @DisplayName("get last seen notification date")
  fun verifyAndDocumentGetLastSeenNotification() {
    userService.setLastSeen(fmUser.identifier, Date())

    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(get(latestVersionOf(NOTIFICATION_LAST_SEEN_ENDPOINT)), objectMapper))
          .andExpect(status().isOk)
          .andDo(
              document(
                  "get-last-seen-notification-date",
                  responseFields(
                      fieldWithPath("lastSeen").description("The last seen date"),
                      subsectionWithPath("_links").ignored())))
    }
  }

  @Test
  @DisplayName("set last seen notification date")
  fun verifyAndDocumentSetLastSeenNotification() {
    val lastSeenDate = Date()
    val lastSeenResource = UpdateLastSeenResource(lastSeenDate)

    val constrainedField = ConstrainedFields(UpdateLastSeenResource::class.java)

    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  post(latestVersionOf(NOTIFICATION_LAST_SEEN_ENDPOINT)),
                  lastSeenResource,
                  objectMapper))
          .andExpect(status().isNoContent)
          .andDo(
              document(
                  "set-last-seen-notification-date",
                  requestFields(
                      constrainedField
                          .withPath("lastSeen")
                          .description("The last seen date")
                          .type(STRING))))
    }

    assertThat(repositories.userRepository.findOneCachedByIdentifier(fmUser.identifier))
        .extracting("lastSeen")
        .isEqualTo(lastSeenDate.toInstant().truncatedTo(MILLIS))
  }

  @Test
  @DisplayName("mark a notification as read")
  fun verifyAndDocumentSetNotificationRead() {

    eventStreamGenerator.submitTaskAsFm().submitTopicG2()

    val notification = repositories.notificationRepository.findAll(fmUser.identifier, 1)[0]

    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  post(
                      latestVersionOf(MARK_NOTIFICATION_AS_READ_ENDPOINT),
                      notification.externalIdentifier),
                  objectMapper))
          .andExpect(status().isNoContent)
          .andDo(
              document(
                  "mark-a-notification-as-read",
                  pathParameters(
                      parameterWithName("notificationId").description("ID of the notification"))))
    }

    assertThat(
            repositories.notificationRepository.findOneByExternalIdentifier(
                notification.externalIdentifier!!))
        .extracting("read")
        .isEqualTo(true)
  }

  @Test
  @DisplayName("get notifications")
  fun verifyAndDocumentFindAllNotificationsForUser() {
    val before = Instant.now().plusMillis(10000)
    eventStreamGenerator
        .submitTaskAsFm()
        .submitDayCardG2(asReference = "dayCard", auditUserReference = FM_USER)
        // Create notifications
        .submitDayCardG2(
            asReference = "dayCard",
            eventType = DayCardEventEnumAvro.UPDATED,
            time = before.minusMillis(3000)) {
              it.notes = "update 1"
            }
        .submitDayCardG2(
            asReference = "dayCard",
            eventType = DayCardEventEnumAvro.UPDATED,
            time = before.minusMillis(2000)) {
              it.notes = "update 2"
            }
        .submitDayCardG2(
            asReference = "dayCard",
            eventType = DayCardEventEnumAvro.UPDATED,
            time = before.minusMillis(1000)) {
              it.notes = "update 3"
            }

    // Set lastSeen Date to the point in time between "update 2" and "update 3"
    userService.setLastSeen(fmUser.identifier, Date(before.minusMillis(1500).toEpochMilli()))

    val notifications = notificationService.findAll(fmUser.identifier, 3)
    val notificationFromUpdate3 = notifications.resources!![0]
    val notificationFromUpdate2 = notifications.resources!![1]
    val notificationFromUpdate1 = notifications.resources!![2]

    // Mark the notifications from the first and second update as read
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate1.externalIdentifier!!)
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate2.externalIdentifier!!)

    // document with request param 'before'
    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf(
                          "$NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT?before={before}&limit={limit}"),
                      df.format(Date(before.toEpochMilli())),
                      2),
                  objectMapper))
          .andExpect(status().isOk)
          .andExpect(jsonPath("$.items.length()").value(2))
          .andExpect(
              jsonPath("$.items[0].id")
                  .value(notificationFromUpdate3.externalIdentifier.toString()))
          .andExpect(
              jsonPath("$.items[1].id")
                  .value(notificationFromUpdate2.externalIdentifier.toString()))
          .andDo(
              document(
                  "get-notifications",
                  links(linkWithRel("prev").description("Link to the previous page if available")),
                  queryParameters(
                      parameterWithName("before")
                          .description(
                              "Optional parameter to find notification before this date " +
                                  "(meaning older notifications). Can NOT be used in " +
                                  "combination with the after parameter.")
                          .optional(),
                      parameterWithName("after")
                          .description(
                              "Optional parameter to find notification after this date " +
                                  "(meaning more recent notifications. Can NOT be used" +
                                  " in combination with the before parameter")
                          .optional(),
                      parameterWithName("limit")
                          .description(
                              "Optional parameter to limit the number of fetched " +
                                  "notifications. (Max value is 50).")
                          .optional()),
                  responseFields()
                      .andWithPrefix(
                          "items[].",
                          listOf(
                              fieldWithPath("id").description("Identifier of the notification"),
                              fieldWithPath("read").description("Notification is marked as read"),
                              fieldWithPath("actor")
                                  .description("The user who is responsible for the change"),
                              subsectionWithPath("actor.*").ignored(),
                              fieldWithPath("date")
                                  .description("Date when the notification was saved")
                                  .type("Date"),
                              fieldWithPath("summary").description("Summary what was changed"),
                              subsectionWithPath("summary.*").ignored(),
                              fieldWithPath("changes").description("The change").optional(),
                              fieldWithPath("context")
                                  .description("Object references the change is related to"),
                              subsectionWithPath("context.*").ignored(),
                              fieldWithPath("object")
                                  .description("The object references the change belongs to"),
                              subsectionWithPath("object.*").ignored(),
                              subsectionWithPath("_links").ignored()))
                      .and(
                          fieldWithPath("lastSeen")
                              .description("Date the user has marked list as seen the last time"))
                      .and(subsectionWithPath("_links").ignored())))

      // with request param 'after'
      val after = before.minusMillis(30000)
      doWithAuthorization(fmUser) {
        mockMvc
            .perform(
                requestBuilder(
                    get(
                        latestVersionOf(
                            "$NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT?after={after}&limit={limit}"),
                        df.format(Date(after.toEpochMilli())),
                        1),
                    objectMapper))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(
                jsonPath("$.items[0].id")
                    .value(notificationFromUpdate1.externalIdentifier.toString()))
            .andExpect(
                jsonPath("$._links.prev.href")
                    .value(
                        matchesPattern(
                            ".*" +
                                latestVersionOf(
                                    "/projects/notifications\\?after=[.:0-9TZ-]+&limit=1"))))
      }

      // with request param 'after' and no limit
      doWithAuthorization(fmUser) {
        mockMvc
            .perform(
                requestBuilder(
                    get(
                        latestVersionOf("$NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT?after={after}"),
                        df.format(Date(after.toEpochMilli()))),
                    objectMapper))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(
                jsonPath("$.items[0].id")
                    .value(notificationFromUpdate1.externalIdentifier.toString()))
            .andExpect(
                jsonPath("$.items[1].id")
                    .value(notificationFromUpdate2.externalIdentifier.toString()))
            .andExpect(jsonPath("$._links.prev.href").doesNotExist())
      }

      // without limit and without param
      doWithAuthorization(fmUser) {
        mockMvc
            .perform(
                requestBuilder(
                    get(latestVersionOf(NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT)), objectMapper))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(
                jsonPath("$.items[0].id")
                    .value(notificationFromUpdate3.externalIdentifier.toString()))
            .andExpect(
                jsonPath("$.items[1].id")
                    .value(notificationFromUpdate2.externalIdentifier.toString()))
            .andExpect(
                jsonPath("$.items[2].id")
                    .value(notificationFromUpdate1.externalIdentifier.toString()))
            .andExpect(jsonPath("$._links.prev.href").doesNotExist())
      }
    }
  }

  @Test
  @DisplayName("get notifications without limit")
  fun verifyAndDocumentFindAllNotificationsWithoutLimitForUser() {
    val before = Instant.now().plusMillis(10000)
    eventStreamGenerator
        .submitTaskAsFm()
        .submitDayCardG2(asReference = "dayCard", auditUserReference = FM_USER)
    // Create many! notifications
    for (i in 1..70) {
      eventStreamGenerator.submitDayCardG2(
          asReference = "dayCard",
          eventType = DayCardEventEnumAvro.UPDATED,
          time = before.minusMillis((1000 + (i * 1000)).toLong())) {
            it.notes = "update $i"
          }
    }

    // Set lastSeen Date to the point in time between "update 2" and "update 3"
    userService.setLastSeen(fmUser.identifier, Date(before.minusMillis(2500).toEpochMilli()))

    val notifications = notificationService.findAll(fmUser.identifier, 50)

    val notificationFromUpdate3 = notifications.resources!![0]
    val notificationFromUpdate2 = notifications.resources!![1]
    val notificationFromUpdate1 = notifications.resources!![2]

    // Mark the notifications from the first and second update as read
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate1.externalIdentifier!!)
    notificationService.markAsRead(fmUser.identifier, notificationFromUpdate2.externalIdentifier!!)

    // with request param 'before' and no limit
    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf("$NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT?before={before}"),
                      df.format(Date(before.toEpochMilli()))),
                  objectMapper))
          .andExpect(status().isOk)
          .andExpect(jsonPath("$.items.length()").value(50))
          .andExpect(
              jsonPath("$.items[0].id")
                  .value(notificationFromUpdate3.externalIdentifier.toString()))
          .andExpect(
              jsonPath("$.items[1].id")
                  .value(notificationFromUpdate2.externalIdentifier.toString()))
          .andExpect(
              jsonPath("$.items[2].id")
                  .value(notificationFromUpdate1.externalIdentifier.toString()))
          .andExpect(
              jsonPath("$._links.prev.href")
                  .value(
                      matchesPattern(
                          ".*" +
                              latestVersionOf(
                                  "/projects/notifications\\?before=[.:0-9TZ-]+&limit=50"))))
    }

    // with request param 'before' and no limit
    val after = before.minusMillis(1000 * 60) // should put the starting point ~ at 1/7th of items
    doWithAuthorization(fmUser) {
      mockMvc
          .perform(
              requestBuilder(
                  get(
                      latestVersionOf("$NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT?after={after}"),
                      df.format(Date(after.toEpochMilli()))),
                  objectMapper))
          .andExpect(status().isOk)
          .andExpect(jsonPath("$.items.length()").value(50))
          .andExpect(
              jsonPath("$._links.prev.href")
                  .value(
                      matchesPattern(
                          ".*" +
                              latestVersionOf(
                                  "/projects/notifications\\?after=[.:0-9TZ-]+&limit=50"))))
    }
  }
}
