/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChange
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChangesCount
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED_MANPOWER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED_NOTE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED_TITLE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class DayCardCreatedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
    }
  }

  @Test
  fun `when a day card is created`() {
    eventStreamGenerator.submitDayCardG2 {
      it.title = "Daycard Title"
      it.manpower = 1F.toBigDecimal()
      it.notes = "Daycard Note"
      it.reason = null
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 3))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_CREATED_TITLE, "Daycard Title"),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_CREATED_MANPOWER, 1F.toBigDecimal().toString()),
                changeIndex = 1))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_CREATED_NOTE, "Daycard Note"), changeIndex = 2))
  }

  @Test
  fun `when a day card is created with no notes`() {
    eventStreamGenerator.submitDayCardG2 {
      it.title = "Daycard Title"
      it.manpower = 1F.toBigDecimal()
      it.notes = null
      it.reason = null
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 2))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_CREATED_TITLE, "Daycard Title"),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_CREATED_MANPOWER, 1F.toBigDecimal().toString()),
                changeIndex = 1))
  }

  private fun buildSummary() =
      buildSummary(
          messageKey = DAY_CARD_ACTIVITY_CREATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
                  "daycard" to buildPlaceholder(getByReference("dayCard"), "Daycard Title")))
}
