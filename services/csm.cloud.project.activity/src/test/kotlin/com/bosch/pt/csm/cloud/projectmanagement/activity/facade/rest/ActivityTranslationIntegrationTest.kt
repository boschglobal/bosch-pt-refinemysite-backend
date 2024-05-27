/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasActivitiesCount
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import java.util.Locale
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SmartSiteSpringBootTest
@DisplayName("Verify activity api")
class ActivityTranslationIntegrationTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    LocaleContextHolder.setLocale(null)
    eventStreamGenerator.setUserContext("csm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
      it.status = TaskStatusEnumAvro.OPEN
      // setting all non-mandatory fields to null
      it.location = null
      it.description = null
    }
  }

  @Test
  fun `returns messages in the correct language with locale from user`() {
    val expectedLocale = Locale.GERMANY

    eventStreamGenerator.submitUser(
        asReference = "csm-user", eventType = UserEventEnumAvro.UPDATED) {
      it.locale = expectedLocale.toString()
    }

    requestActivities(task = task, locale = null, limit = 50)
        .andExpectOk()
        .andExpect(hasActivitiesCount(1))
        .andExpect(hasSummary(index = 0, summary = buildSummaryForTask(expectedLocale)))
  }

  @Test
  fun `returns messages in the correct language with locale from request`() {
    val expectedLocale = Locale.FRANCE

    eventStreamGenerator.submitUser(
        asReference = "csm-user", eventType = UserEventEnumAvro.UPDATED) {
      it.locale = Locale.GERMANY.toString()
    }

    requestActivities(task = task, locale = expectedLocale, limit = 50)
        .andExpectOk()
        .andExpect(hasActivitiesCount(1))
        .andExpect(hasSummary(index = 0, summary = buildSummaryForTask(expectedLocale)))
  }

  @Test
  fun `returns messages in the correct language with default locale`() {
    eventStreamGenerator.submitUser(
        asReference = "csm-user", eventType = UserEventEnumAvro.UPDATED) { it.locale = null }

    requestActivities(task = task, locale = null, limit = 50)
        .andExpectOk()
        .andExpect(hasActivitiesCount(1))
        .andExpect(hasSummary(index = 0, summary = buildSummaryForTask(Locale.UK)))
  }

  private fun buildSummaryForTask(expectedTranslation: Locale): SummaryDto {
    val originalLocale = LocaleContextHolder.getLocale()
    LocaleContextHolder.setLocale(expectedTranslation)

    val summary =
        buildSummary(
            messageKey = TASK_ACTIVITY_CREATED,
            objectReferences =
                mapOf(
                    "originator" to
                        buildPlaceholder(
                            csmParticipant.getAggregateIdentifier(), csmUser.displayName()),
                    "task" to buildPlaceholder(task.getAggregateIdentifier(), task.getName())))

    LocaleContextHolder.setLocale(originalLocale)
    return summary
  }

  fun requestActivities(
      task: TaskAggregateAvro,
      authorizeAsUser: UserAggregateAvro = csmUser,
      locale: Locale?,
      limit: Int = 1
  ): ResultActions =
      doWithAuthorization(repositories.findUser(authorizeAsUser)) {
        mockMvc.perform(
            // Normally this call would be wrapped by RestUtilsKt.requestBuilder(...)
            // but it is necessary to directly call the MockHttpServletRequestBuilder
            // to be able to not set the language header and locale.
            get(
                latestVersionOf("/projects/tasks/{taskId}/activities?limit={limit}"),
                task.getIdentifier(),
                limit)
                .locale(locale)
                .header(ACCEPT, HAL_JSON_VALUE)
                .let { if (locale == null) it else it.header(ACCEPT_LANGUAGE, locale.toString()) })
      }
}
