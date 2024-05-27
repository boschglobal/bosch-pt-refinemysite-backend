/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasActivitiesCount
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class ActivityCreatedByTestadminIntegrationTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Notes of the daycard"
          it.reason = null
        }
  }

  @Test
  fun `created by the test admin contains the correct summary`() {
    requestActivities(task = task, limit = 2)
        .andExpectOk()
        .andExpect(hasActivitiesCount(2))
        .andExpect(hasSummary(summary = buildSummaryForDayCard(), index = 0))
        .andExpect(hasSummary(summary = buildSummaryForTask(), index = 1))
  }

  private fun buildSummaryForTask() =
      buildSummary(
          messageKey = TASK_ACTIVITY_CREATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          testAdminUser.getAggregateIdentifier(), testAdminUser.displayName()),
                  "task" to buildPlaceholder(task.getAggregateIdentifier(), task.getName())))

  private fun buildSummaryForDayCard() =
      buildSummary(
          messageKey = DAY_CARD_ACTIVITY_CREATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          testAdminUser.getAggregateIdentifier(), testAdminUser.displayName()),
                  "daycard" to buildPlaceholder(getByReference("dayCard"), "Daycard Title")))
}
