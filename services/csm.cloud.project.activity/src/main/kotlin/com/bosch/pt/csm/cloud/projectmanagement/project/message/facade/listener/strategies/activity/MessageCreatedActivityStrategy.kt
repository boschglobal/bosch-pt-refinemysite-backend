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
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.NoDetails
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.BARE_PARAMETERS_ONE_PARAMETER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ACTIVITY_CREATED_TOPIC_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.message.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.CREATED
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
class MessageCreatedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val participantService: ParticipantService,
    private val topicService: TopicService
) : AbstractActivityStrategy<MessageEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MessageEventAvro && value.getName() == CREATED

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
          details = buildDetails(this),
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

    val topicDescription = topic.description

    return if (topicDescription != null) {
      val resolvedTopic =
          ResolvedObjectReference(
              type = TOPIC.type,
              identifier = messageAggregateAvro.getTopicIdentifier(),
              displayName = topicDescription)

      Summary(
          templateMessageKey = MESSAGE_ACTIVITY_CREATED,
          references = mapOf("originator" to originator, "topic" to resolvedTopic))
    } else {
      Summary(
          templateMessageKey = MESSAGE_ACTIVITY_CREATED_TOPIC_WITHOUT_TEXT,
          references = mapOf("originator" to originator))
    }
  }

  private fun buildDetails(messageAggregateAvro: MessageAggregateAvro) =
      messageAggregateAvro.getContent().let {
        when (it) {
          null -> NoDetails()
          else ->
              AttributeChanges(
                  listOf(
                      ChangeDescription(BARE_PARAMETERS_ONE_PARAMETER, listOf(SimpleString(it)))))
        }
      }
}
