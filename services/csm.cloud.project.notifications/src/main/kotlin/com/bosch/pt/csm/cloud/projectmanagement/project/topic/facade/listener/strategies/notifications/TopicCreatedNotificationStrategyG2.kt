/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TOPIC_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.AbstractNotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TopicCreatedNotificationStrategyG2(
    private val participantService: ParticipantService,
    private val taskService: TaskService,
    private val recipientDeterminator: RecipientDeterminator
) : AbstractNotificationStrategy<TopicEventG2Avro>() {

  override fun handles(record: EventRecord) =
      record.value is TopicEventG2Avro &&
          (record.value as TopicEventG2Avro).name == TopicEventEnumAvro.CREATED

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TopicEventG2Avro
  ): Set<Notification> {
    val aggregate = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipients = determineRecipients(aggregate, projectIdentifier)

    return if (recipients.isEmpty()) {
      emptySet()
    } else {
      recipients
          .map { recipient ->
            Notification(
                notificationIdentifier = buildNotificationIdentifier(aggregate, recipient),
                event = buildEventInformation(event),
                summary = buildSummary(projectIdentifier, event),
                details = buildDetails(aggregate),
                context = buildContext(projectIdentifier, aggregate))
          }
          .toSet()
    }
  }

  private fun determineRecipients(
      topicAggregateAvro: TopicAggregateG2Avro,
      projectIdentifier: UUID
  ): Set<UUID> {
    val lastModifiedByUserIdentifier =
        topicAggregateAvro.auditingInformation.lastModifiedBy.identifier.toUUID()

    val task =
        taskService.findLatest(topicAggregateAvro.task.identifier.toUUID(), projectIdentifier)

    return recipientDeterminator.determineDefaultRecipients(task, lastModifiedByUserIdentifier)
  }

  private fun buildNotificationIdentifier(
      aggregate: TopicAggregateG2Avro,
      recipientIdentifier: UUID
  ) = aggregate.buildNotificationIdentifier(recipientIdentifier)

  private fun buildEventInformation(topicEventAvro: TopicEventG2Avro) =
      EventInformation(
          name = topicEventAvro.name.name,
          date = topicEventAvro.aggregate.getLastModifiedDate(),
          user = topicEventAvro.aggregate.getLastModifiedByUserIdentifier())

  private fun buildSummary(
      projectIdentifier: UUID,
      topicEventAvro: TopicEventG2Avro
  ): TemplateWithPlaceholders {
    val userIdentifier = topicEventAvro.aggregate.getLastModifiedByUserIdentifier()

    val actorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = actorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return TemplateWithPlaceholders(
        templateMessageKey = NOTIFICATION_SUMMARY_TOPIC_CREATED,
        placeholderAggregateReferenceValues = mapOf(Pair("originator", originator)))
  }

  private fun buildDetails(topicAggregateAvro: TopicAggregateG2Avro) =
      topicAggregateAvro.description?.let { SimpleString("\"${topicAggregateAvro.description}\"") }

  private fun buildContext(projectIdentifier: UUID, topicAggregateAvro: TopicAggregateG2Avro) =
      Context(project = projectIdentifier, task = topicAggregateAvro.task.identifier.toUUID())
}
