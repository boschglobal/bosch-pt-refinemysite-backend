/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_ESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_ESCALATED_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TopicEscalatedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val participantService: ParticipantService
) : AbstractActivityStrategy<TopicEventG2Avro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TopicEventG2Avro && value.getName() == ESCALATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TopicEventG2Avro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, this),
          context = Context(project = projectIdentifier, task = this.getTaskIdentifier()))
    }
  }
  private fun buildSummary(
      projectIdentifier: UUID,
      topicAggregateAvro: TopicAggregateG2Avro
  ): Summary {
    val userIdentifier = topicAggregateAvro.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, userIdentifier)

    val originator =
        UnresolvedObjectReference(
            type = PARTICIPANT.type,
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return when (topicAggregateAvro.getDescription()) {
      null ->
          Summary(
              templateMessageKey = TOPIC_ACTIVITY_ESCALATED_WITHOUT_TEXT,
              references = mapOf("originator" to originator))
      else ->
          Summary(
              templateMessageKey = TOPIC_ACTIVITY_ESCALATED,
              references =
                  mapOf(
                      "originator" to originator,
                      "topic" to
                          ResolvedObjectReference(
                              type = TOPIC.type,
                              identifier = topicAggregateAvro.getIdentifier(),
                              displayName = topicAggregateAvro.getDescription())))
    }
  }
}
