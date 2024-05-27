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
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChange
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CANCELLED_REASON
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_BADWEATHER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_CHANGEDPRIORITY
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_CONCESSIONNOTRECOGNIZED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_DELAYEDMATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_MANPOWERSHORTAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_MISSINGINFOS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_MISSINGTOOLS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_NOCONCESSION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_OVERESTIMATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_TOUCHUP
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.BAD_WEATHER
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CHANGED_PRIORITY
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CONCESSION_NOT_RECOGNIZED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM3
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM4
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.DELAYED_MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MANPOWER_SHORTAGE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MISSING_INFOS
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MISSING_TOOLS
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.NO_CONCESSION
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.OVERESTIMATION
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.TOUCHUP
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class DayCardCancelledActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .submitDayCardG2()
        .submitRfvCustomization(asReference = "rfvCustomization-custom1") {
          it.name = CUSTOM_RFV_NAME
          it.key = CUSTOM1
          it.active = true
        }
        .submitRfvCustomization("rfvCustomization-custom2") {
          it.key = CUSTOM2
          it.active = false
        }
  }

  @ParameterizedTest
  @EnumSource(DayCardReasonNotDoneEnumAvro::class)
  fun `when day card is cancelled with a reason`(reason: DayCardReasonNotDoneEnumAvro) {

    eventStreamGenerator.submitDayCardG2(eventType = CANCELLED) {
      it.status = OPEN
      it.title = "Daycard Title"
      it.reason = reason
    }

    val summary =
        buildSummary(
            messageKey = DAY_CARD_ACTIVITY_CANCELLED,
            objectReferences =
                mapOf(
                    "originator" to
                        buildPlaceholder(
                            fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
                    "daycard" to buildPlaceholder(getByReference("dayCard"), "Daycard Title")))

    val details =
        if (DayCardReasonEnum.valueOf(reason.name).isCustom()) {
          when (reason) {
            CUSTOM1 -> translate(DAY_CARD_ACTIVITY_CANCELLED_REASON, CUSTOM_RFV_NAME)
            CUSTOM2, CUSTOM3, CUSTOM4 ->
                translate(DAY_CARD_ACTIVITY_CANCELLED_REASON, translate(mapReasonToKey(reason)))
            else -> throw IllegalArgumentException("Unexpected reason: $reason")
          }
        } else {
          translate(DAY_CARD_ACTIVITY_CANCELLED_REASON, translate(mapReasonToKey(reason)))
        }

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(fmUser))
        .andExpect(ActivityMatchers.hasSummary(summary))
        .andExpect(ActivityMatchers.hasChangesCount(count = 1))
        .andExpect(hasChange(text = details))
  }

  private fun mapReasonToKey(reason: DayCardReasonNotDoneEnumAvro) =
      when (reason) {
        BAD_WEATHER -> DAY_CARD_REASON_ENUM_BADWEATHER
        CHANGED_PRIORITY -> DAY_CARD_REASON_ENUM_CHANGEDPRIORITY
        CONCESSION_NOT_RECOGNIZED -> DAY_CARD_REASON_ENUM_CONCESSIONNOTRECOGNIZED
        DELAYED_MATERIAL -> DAY_CARD_REASON_ENUM_DELAYEDMATERIAL
        MANPOWER_SHORTAGE -> DAY_CARD_REASON_ENUM_MANPOWERSHORTAGE
        MISSING_INFOS -> DAY_CARD_REASON_ENUM_MISSINGINFOS
        MISSING_TOOLS -> DAY_CARD_REASON_ENUM_MISSINGTOOLS
        NO_CONCESSION -> DAY_CARD_REASON_ENUM_NOCONCESSION
        OVERESTIMATION -> DAY_CARD_REASON_ENUM_OVERESTIMATION
        TOUCHUP -> DAY_CARD_REASON_ENUM_TOUCHUP
        CUSTOM1 -> "${DAY_CARD_REASON_ENUM_PREFIX}_CUSTOM1"
        CUSTOM2 -> "${DAY_CARD_REASON_ENUM_PREFIX}_CUSTOM2"
        CUSTOM3 -> "${DAY_CARD_REASON_ENUM_PREFIX}_CUSTOM3"
        CUSTOM4 -> "${DAY_CARD_REASON_ENUM_PREFIX}_CUSTOM4"
      }

  companion object {
    private const val CUSTOM_RFV_NAME = "Custom text"
    private val DAY_CARD_REASON_ENUM_PREFIX = DayCardReasonEnum::class.java.simpleName
  }
}
