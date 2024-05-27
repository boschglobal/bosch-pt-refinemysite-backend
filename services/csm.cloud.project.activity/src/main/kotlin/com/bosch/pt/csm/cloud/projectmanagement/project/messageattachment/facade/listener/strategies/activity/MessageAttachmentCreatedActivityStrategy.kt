/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.messageattachment.facade.listener.strategies.activity

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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ATTACHMENT_ACTIVITY_SAVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MESSAGE_ATTACHMENT_ACTIVITY_SAVED_TOPIC_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.getMessageIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.getMessageVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildAuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.message.model.Message
import com.bosch.pt.csm.cloud.projectmanagement.project.message.service.MessageService
import com.bosch.pt.csm.cloud.projectmanagement.project.messageattachment.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.service.TopicService
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class MessageAttachmentCreatedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val messageService: MessageService,
    private val participantService: ParticipantService,
    private val topicService: TopicService
) : AbstractActivityStrategy<MessageAttachmentEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MessageAttachmentEventAvro && value.getName() == CREATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: MessageAttachmentEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier
    val messageIdentifier = event.getMessageIdentifier()
    val messageVersion = event.getMessageVersion()
    val message = messageService.findMessage(messageIdentifier, messageVersion, projectIdentifier)
    val topic = topicService.findLatest(message.topicIdentifier, projectIdentifier)

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = getAggregateIdentifier().buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event, message, topic),
          details = buildDetails(this),
          context = Context(project = projectIdentifier, task = topic.taskIdentifier),
          attachment = buildAttachment(projectIdentifier, this))
    }
  }
  private fun buildSummary(
      projectIdentifier: UUID,
      messageAttachmentEventAvro: MessageAttachmentEventAvro,
      message: Message,
      topic: Topic
  ): Summary {
    val userIdentifier = messageAttachmentEventAvro.getLastModifiedByUserIdentifier()
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
              identifier = message.topicIdentifier,
              displayName = topicDescription)
      Summary(
          templateMessageKey = MESSAGE_ATTACHMENT_ACTIVITY_SAVED,
          references = mapOf("originator" to originator, "topic" to resolvedTopic))
    } else {
      Summary(
          templateMessageKey = MESSAGE_ATTACHMENT_ACTIVITY_SAVED_TOPIC_WITHOUT_TEXT,
          references = mapOf("originator" to originator))
    }
  }

  private fun buildDetails(messageAttachmentAggregateAvro: MessageAttachmentAggregateAvro) =
      AttributeChanges(
          listOf(
              ChangeDescription(
                  BARE_PARAMETERS_ONE_PARAMETER,
                  listOf(
                      SimpleString(messageAttachmentAggregateAvro.getAttachment().getFileName())))))

  private fun buildAttachment(projectIdentifier: UUID, aggregate: MessageAttachmentAggregateAvro) =
      aggregate
          .getAttachment()
          .buildAttachment(
              auditingInformation =
                  aggregate.getAuditingInformation().buildAuditingInformation(projectIdentifier),
              identifier = aggregate.getIdentifier(),
              messageId = aggregate.getMessageIdentifier())
}
