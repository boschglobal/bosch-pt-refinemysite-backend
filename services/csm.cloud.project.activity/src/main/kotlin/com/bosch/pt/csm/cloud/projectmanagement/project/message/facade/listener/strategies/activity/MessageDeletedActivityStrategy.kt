/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ACTIVITY_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ACTIVITY_DELETED_MESSAGE_AND_TOPIC_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ACTIVITY_DELETED_MESSAGE_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.message.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.service.TopicService
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class MessageDeletedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val participantService: ParticipantService,
    private val topicService: TopicService
) : AbstractActivityStrategy<MessageEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MessageEventAvro && value.getName() == DELETED

  @Trace
  override fun createActivity(key: EventMessageKey, event: MessageEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier
    val aggregate = event.getAggregate()
    val topic = topicService.findLatest(aggregate.getTopicIdentifier(), projectIdentifier)

    return aggregate.run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, this, topic),
          context = Context(project = projectIdentifier, task = topic.taskIdentifier))
    }
  }
  private fun buildSummary(
      projectIdentifier: UUID,
      messageAggregateAvro: MessageAggregateAvro,
      topic: Topic
  ): Summary {
    val userIdentifier = messageAggregateAvro.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, userIdentifier)

    val originator =
        UnresolvedObjectReference(
            type = PARTICIPANT.type,
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    val messageContent = messageAggregateAvro.getContent()
    val topicDescription = topic.description

    return when {
      messageContent == null && topicDescription == null ->
          Summary(
              templateMessageKey = MESSAGE_ACTIVITY_DELETED_MESSAGE_AND_TOPIC_WITHOUT_TEXT,
              references = mapOf("originator" to originator))
      messageContent == null && topicDescription != null ->
          Summary(
              templateMessageKey = MESSAGE_ACTIVITY_DELETED_MESSAGE_WITHOUT_TEXT,
              references =
                  mapOf(
                      "originator" to originator,
                      "topic" to
                          ResolvedObjectReference(
                              type = TOPIC.type,
                              identifier = messageAggregateAvro.getTopicIdentifier(),
                              displayName = topicDescription)))
      else ->
          Summary(
              templateMessageKey = MESSAGE_ACTIVITY_DELETED,
              references =
                  mapOf(
                      "originator" to originator,
                      "comment" to
                          ResolvedObjectReference(
                              type = MESSAGE.type,
                              identifier = messageAggregateAvro.getIdentifier(),
                              displayName = messageContent)))
    }
  }
}
