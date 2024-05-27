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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED_NOTE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_MANPOWER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_NOTE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_NOTE_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_TITLE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity when")
@SmartSiteSpringBootTest
class DayCardUpdatedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
    }
  }

  @Test
  fun `adding notes to a day card`() {
    eventStreamGenerator
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = null
          it.reason = null
        }
        .submitDayCardG2(eventType = UPDATED) { it.notes = "adding notes" }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_CREATED_NOTE, "adding notes"), changeIndex = 0))
  }

  @Test
  fun `updating notes of a day card`() {
    eventStreamGenerator
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Note"
          it.reason = null
        }
        .submitDayCardG2(eventType = UPDATED) { it.notes = "update notes" }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_UPDATED_NOTE, "Daycard Note", "update notes"),
                changeIndex = 0))
  }

  @Test
  fun `removing notes from a day card`() {
    eventStreamGenerator
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Note"
          it.reason = null
        }
        .submitDayCardG2(eventType = UPDATED) { it.notes = null }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_UPDATED_NOTE_REMOVED, "Daycard Note"),
                changeIndex = 0))
  }

  @Test
  fun `updating title of a day card`() {
    eventStreamGenerator
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Note"
          it.reason = null
        }
        .submitDayCardG2(eventType = UPDATED) { it.title = "update title" }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary("update title")))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(DAY_CARD_ACTIVITY_UPDATED_TITLE, "Daycard Title", "update title"),
                changeIndex = 0))
  }

  @Test
  fun `updating manpower field of a day card`() {
    eventStreamGenerator
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Note"
          it.reason = null
        }
        .submitDayCardG2(eventType = UPDATED) { it.manpower = 2F.toBigDecimal() }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        DAY_CARD_ACTIVITY_UPDATED_MANPOWER,
                        1F.toBigDecimal().stripTrailingZeros().toString(),
                        2F.toBigDecimal().stripTrailingZeros().toString()),
                changeIndex = 0))
  }

  private fun buildSummary(title: String = "Daycard Title") =
      buildSummary(
          messageKey = DAY_CARD_ACTIVITY_UPDATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
                  "daycard" to buildPlaceholder(getByReference("dayCard"), title)))
}
