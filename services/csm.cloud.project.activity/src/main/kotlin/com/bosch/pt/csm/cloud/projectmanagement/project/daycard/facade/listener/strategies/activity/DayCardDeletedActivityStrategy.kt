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
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.getDisplayName
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class DayCardDeletedActivityStrategy(
    private val participantService: ParticipantService,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<DayCardEventG2Avro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is DayCardEventG2Avro && value.getName() == DELETED

  @Trace
  override fun createActivity(key: EventMessageKey, event: DayCardEventG2Avro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event),
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
        templateMessageKey = DAY_CARD_ACTIVITY_DELETED,
        references = mapOf("originator" to originator, "daycard" to dayCard))
  }
}
