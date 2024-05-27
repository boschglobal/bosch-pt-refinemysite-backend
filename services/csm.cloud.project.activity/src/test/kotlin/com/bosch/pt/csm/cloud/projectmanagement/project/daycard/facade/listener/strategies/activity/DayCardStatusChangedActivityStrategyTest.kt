/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_COMPLETED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_RESET
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.COMPLETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class DayCardStatusChangedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("csm-user")
        .submitTask(auditUserReference = "fm-user") {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .submitDayCardG2()
  }

  @Test
  fun `when day card is completed`() {
    eventStreamGenerator.submitDayCardG2(eventType = COMPLETED, auditUserReference = "fm-user") {
      it.status = DONE
      it.title = "Daycard Title"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(fmUser))
        .andExpect(ActivityMatchers.hasSummary(buildSummary(DAY_CARD_ACTIVITY_COMPLETED)))
        .andExpect(ActivityMatchers.hasNoChanges())
  }

  @Test
  fun `when day card is approved`() {
    eventStreamGenerator.submitDayCardG2(eventType = APPROVED) {
      it.status = DayCardStatusEnumAvro.APPROVED
      it.title = "Daycard Title"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(csmUser))
        .andExpect(
            ActivityMatchers.hasSummary(
                buildSummary(DAY_CARD_ACTIVITY_APPROVED, csmParticipant, csmUser)))
        .andExpect(ActivityMatchers.hasNoChanges())
  }

  @Test
  fun `when day card status is reset`() {
    eventStreamGenerator.submitDayCardG2(eventType = RESET) {
      it.status = OPEN
      it.title = "Daycard Title"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(csmUser))
        .andExpect(
            ActivityMatchers.hasSummary(
                buildSummary(DAY_CARD_ACTIVITY_RESET, csmParticipant, csmUser)))
        .andExpect(ActivityMatchers.hasNoChanges())
  }

  private fun buildSummary(
      messageKey: String,
      participant: ParticipantAggregateG3Avro = fmParticipant,
      user: UserAggregateAvro = fmUser
  ) =
      buildSummary(
          messageKey = messageKey,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(participant.getAggregateIdentifier(), user.displayName()),
                  "daycard" to buildPlaceholder(getByReference("dayCard"), "Daycard Title")))
}
