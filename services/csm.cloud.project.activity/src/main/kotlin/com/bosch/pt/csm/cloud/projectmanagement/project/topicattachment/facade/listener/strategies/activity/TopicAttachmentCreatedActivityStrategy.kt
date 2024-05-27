/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topicattachment.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.BARE_PARAMETERS_ONE_PARAMETER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ATTACHMENT_ACTIVITY_SAVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ATTACHMENT_ACTIVITY_SAVED_TOPIC_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildAuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.service.TopicService
import com.bosch.pt.csm.cloud.projectmanagement.project.topicattachment.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.getTopicIdentifier
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TopicAttachmentCreatedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val participantService: ParticipantService,
    private val topicService: TopicService
) : AbstractActivityStrategy<TopicAttachmentEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TopicAttachmentEventAvro && value.getName() == CREATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TopicAttachmentEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier
    val topic = topicService.findLatest(event.getTopicIdentifier(), projectIdentifier)

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = getAggregateIdentifier().buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event, topic),
          details = buildDetails(this),
          context = Context(project = projectIdentifier, task = topic.taskIdentifier),
          attachment = buildAttachment(projectIdentifier, this))
    }
  }

  private fun buildSummary(
      projectIdentifier: UUID,
      event: TopicAttachmentEventAvro,
      topic: Topic
  ): Summary {
    val userIdentifier = event.getLastModifiedByUserIdentifier()
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
              identifier = event.getTopicIdentifier(),
              displayName = topicDescription)
      Summary(
          templateMessageKey = TOPIC_ATTACHMENT_ACTIVITY_SAVED,
          references = mapOf("originator" to originator, "topic" to resolvedTopic))
    } else {
      Summary(
          templateMessageKey = TOPIC_ATTACHMENT_ACTIVITY_SAVED_TOPIC_WITHOUT_TEXT,
          references = mapOf("originator" to originator))
    }
  }

  private fun buildDetails(aggregate: TopicAttachmentAggregateAvro) =
      AttributeChanges(
          listOf(
              ChangeDescription(
                  BARE_PARAMETERS_ONE_PARAMETER,
                  listOf(SimpleString(aggregate.getAttachment().getFileName())))))

  private fun buildAttachment(projectIdentifier: UUID, aggregate: TopicAttachmentAggregateAvro) =
      aggregate
          .getAttachment()
          .buildAttachment(
              auditingInformation =
                  aggregate.getAuditingInformation().buildAuditingInformation(projectIdentifier),
              identifier = aggregate.getIdentifier(),
              topicId = aggregate.getTopicIdentifier())
}
