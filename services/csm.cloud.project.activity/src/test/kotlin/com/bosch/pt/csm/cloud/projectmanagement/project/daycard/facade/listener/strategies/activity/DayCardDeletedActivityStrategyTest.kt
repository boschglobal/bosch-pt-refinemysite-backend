/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasNoChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class DayCardDeletedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitTask(auditUserReference = "fm-user") {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .setUserContext("csm-user")
        .submitDayCardG2 {
          it.status = OPEN
          it.title = "Daycard Title"
          it.reason = null
        }
  }

  @Test
  fun `when day card is deleted`() {
    eventStreamGenerator.submitDayCardG2(eventType = DELETED)

    val summary =
        buildSummary(
            messageKey = DAY_CARD_ACTIVITY_DELETED,
            objectReferences =
                mapOf(
                    "originator" to
                        buildPlaceholder(
                            csmParticipant.getAggregateIdentifier(), csmUser.displayName()),
                    "daycard" to buildPlaceholder(getByReference("dayCard"), "Daycard Title")))

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(csmUser))
        .andExpect(hasSummary(summary))
        .andExpect(hasNoChanges())
  }
}
