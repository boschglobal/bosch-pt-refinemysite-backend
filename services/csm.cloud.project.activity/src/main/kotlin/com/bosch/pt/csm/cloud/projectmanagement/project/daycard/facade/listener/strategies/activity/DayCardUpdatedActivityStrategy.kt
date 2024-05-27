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
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED_NOTE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_MANPOWER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_NOTE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_NOTE_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_UPDATED_TITLE
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getDayCardVersion
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AggregateComparator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.getDisplayName
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.service.DayCardService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class DayCardUpdatedActivityStrategy(
    private val participantService: ParticipantService,
    private val dayCardService: DayCardService,
    private val aggregateComparator: AggregateComparator,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<DayCardEventG2Avro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is DayCardEventG2Avro && value.getName() == UPDATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: DayCardEventG2Avro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event),
          details = buildDetails(projectIdentifier, this),
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
        templateMessageKey = DAY_CARD_ACTIVITY_UPDATED,
        references = mapOf("originator" to originator, "daycard" to dayCard))
  }

  private fun buildDetails(projectIdentifier: UUID, aggregate: DayCardAggregateG2Avro): Details {
    val changedAttributes = differenceToPrevious(projectIdentifier, aggregate)

    return AttributeChanges(
        attributes =
            changedAttributes.map {
              ChangeDescription(
                  templateMessageKey = mapToMessageKey(it.attribute, it.changeType),
                  values = mapToMessageValues(it))
            })
  }

  private fun differenceToPrevious(
      projectIdentifier: UUID,
      aggregate: DayCardAggregateG2Avro
  ): Set<AttributeChange> {
    val identifier = aggregate.getIdentifier()
    val version = aggregate.getDayCardVersion()
    val currentVersion = dayCardService.find(identifier, version, projectIdentifier)
    val previousVersion = dayCardService.find(identifier, version - 1, projectIdentifier)

    val changedAttributes = aggregateComparator.compare(currentVersion, previousVersion)

    return changedAttributes.map(Map.Entry<String, AttributeChange>::value).toSet()
  }

  private fun mapToMessageKey(attribute: String, changeType: AttributeChangeEnum) =
      when (changeType) {
        AttributeChangeEnum.CREATED ->
            when (attribute) {
              "notes" -> DAY_CARD_ACTIVITY_CREATED_NOTE
              else -> throw IllegalStateException("Unknown created day card attribute: $attribute")
            }
        AttributeChangeEnum.UPDATED ->
            when (attribute) {
              "title" -> DAY_CARD_ACTIVITY_UPDATED_TITLE
              "manpower" -> DAY_CARD_ACTIVITY_UPDATED_MANPOWER
              "notes" -> DAY_CARD_ACTIVITY_UPDATED_NOTE
              else -> throw IllegalStateException("Unknown updated day card attribute: $attribute")
            }
        AttributeChangeEnum.REMOVED ->
            when (attribute) {
              "notes" -> DAY_CARD_ACTIVITY_UPDATED_NOTE_REMOVED
              else -> throw IllegalStateException("Unknown removed day card attribute: $attribute")
            }
      }

  private fun mapToMessageValues(attribute: AttributeChange) =
      listOfNotNull(attribute.oldValue, attribute.newValue).map { SimpleString(it as String) }
}
