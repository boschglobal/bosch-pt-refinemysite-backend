/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_COMMENT_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.AbstractNotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.boundary.TopicService
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class MessageCreatedNotificationStrategy(
    private val participantService: ParticipantService,
    private val taskService: TaskService,
    private val topicService: TopicService,
    private val recipientDeterminator: RecipientDeterminator
) : AbstractNotificationStrategy<MessageEventAvro>() {

  override fun handles(record: EventRecord): Boolean {
    return record.value is MessageEventAvro &&
        (record.value as MessageEventAvro).name == MessageEventEnumAvro.CREATED
  }

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: MessageEventAvro
  ): Set<Notification> {
    val messageAggregate = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipients = determineRecipients(messageAggregate, projectIdentifier)

    return if (recipients.isEmpty()) {
      emptySet()
    } else {
      recipients
          .map { recipient ->
            Notification(
                notificationIdentifier = buildNotificationIdentifier(messageAggregate, recipient),
                event = buildEventInformation(event),
                summary = buildSummary(projectIdentifier, event),
                details = buildDetails(messageAggregate),
                context = buildContext(projectIdentifier, messageAggregate))
          }
          .toSet()
    }
  }

  private fun determineRecipients(
      messageAggregate: MessageAggregateAvro,
      projectIdentifier: UUID
  ): Set<UUID> {
    val lastModifiedByUserIdentifier = messageAggregate.getLastModifiedByUserIdentifier()

    val topic = topicService.findLatest(messageAggregate.getTopicIdentifier(), projectIdentifier)
    val task = taskService.findLatest(topic.taskIdentifier, projectIdentifier)

    return recipientDeterminator.determineDefaultRecipients(task, lastModifiedByUserIdentifier)
  }

  private fun buildNotificationIdentifier(
      aggregate: MessageAggregateAvro,
      recipientIdentifier: UUID
  ) = aggregate.buildNotificationIdentifier(recipientIdentifier)

  private fun buildEventInformation(messageEvent: MessageEventAvro) =
      EventInformation(
          name = messageEvent.name.name,
          date = messageEvent.aggregate.getLastModifiedDate(),
          user = messageEvent.aggregate.getLastModifiedByUserIdentifier())

  private fun buildSummary(
      projectIdentifier: UUID,
      messageEvent: MessageEventAvro
  ): TemplateWithPlaceholders {
    val userIdentifier = messageEvent.aggregate.getLastModifiedByUserIdentifier()

    val actorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = actorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return TemplateWithPlaceholders(
        templateMessageKey = NOTIFICATION_SUMMARY_COMMENT_CREATED,
        placeholderAggregateReferenceValues = mapOf("originator" to originator))
  }

  private fun buildDetails(messageAggregate: MessageAggregateAvro): Details? {
    return if (messageAggregate.content == null) {
      null
    } else {
      SimpleString("\"" + messageAggregate.content + "\"")
    }
  }

  private fun buildContext(
      projectIdentifier: UUID,
      messageAggregate: MessageAggregateAvro
  ): Context {
    val topic = topicService.findLatest(messageAggregate.getTopicIdentifier(), projectIdentifier)
    return Context(project = projectIdentifier, task = topic.taskIdentifier)
  }
}
