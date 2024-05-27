/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
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
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.getDisplayName
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
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
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class DayCardCancelledActivityStrategy(
    private val participantService: ParticipantService,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<DayCardEventG2Avro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is DayCardEventG2Avro && value.getName() == CANCELLED

  @Trace
  override fun createActivity(key: EventMessageKey, event: DayCardEventG2Avro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event),
          details = buildDetails(this),
          context = buildContext(projectIdentifier))
    }
  }

  private fun buildSummary(projectIdentifier: UUID, event: DayCardEventG2Avro): Summary {
    val userIdentifier = event.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, userIdentifier)

    val originator =
        UnresolvedObjectReference(
            type = PARTICIPANT.type,
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    val dayCard =
        ResolvedObjectReference(
            type = DAYCARD.type,
            identifier = event.getIdentifier(),
            displayName = event.getDisplayName())

    return Summary(
        templateMessageKey = DAY_CARD_ACTIVITY_CANCELLED,
        references = mapOf("originator" to originator, "daycard" to dayCard))
  }

  private fun buildDetails(aggregate: DayCardAggregateG2Avro): Details {
    val reason =
        when (aggregate.getReason()) {
          BAD_WEATHER -> SimpleMessageKey(DAY_CARD_REASON_ENUM_BADWEATHER)
          CHANGED_PRIORITY -> SimpleMessageKey(DAY_CARD_REASON_ENUM_CHANGEDPRIORITY)
          CONCESSION_NOT_RECOGNIZED ->
              SimpleMessageKey(DAY_CARD_REASON_ENUM_CONCESSIONNOTRECOGNIZED)
          DELAYED_MATERIAL -> SimpleMessageKey(DAY_CARD_REASON_ENUM_DELAYEDMATERIAL)
          MANPOWER_SHORTAGE -> SimpleMessageKey(DAY_CARD_REASON_ENUM_MANPOWERSHORTAGE)
          MISSING_INFOS -> SimpleMessageKey(DAY_CARD_REASON_ENUM_MISSINGINFOS)
          MISSING_TOOLS -> SimpleMessageKey(DAY_CARD_REASON_ENUM_MISSINGTOOLS)
          NO_CONCESSION -> SimpleMessageKey(DAY_CARD_REASON_ENUM_NOCONCESSION)
          OVERESTIMATION -> SimpleMessageKey(DAY_CARD_REASON_ENUM_OVERESTIMATION)
          TOUCHUP -> SimpleMessageKey(DAY_CARD_REASON_ENUM_TOUCHUP)
          CUSTOM1 -> LazyValue(CUSTOM1.name, AggregateType.RFVCUSTOMIZATION.name)
          CUSTOM2 -> LazyValue(CUSTOM2.name, AggregateType.RFVCUSTOMIZATION.name)
          CUSTOM3 -> LazyValue(CUSTOM3.name, AggregateType.RFVCUSTOMIZATION.name)
          CUSTOM4 -> LazyValue(CUSTOM4.name, AggregateType.RFVCUSTOMIZATION.name)
          else ->
              throw IllegalStateException(
                  "Unable to handle day card canceled reason ${aggregate.getReason()}")
        }

    return AttributeChanges(
        attributes =
            listOfNotNull(
                ChangeDescription(
                    templateMessageKey = DAY_CARD_ACTIVITY_CANCELLED_REASON,
                    values = listOf(reason))))
  }
}
