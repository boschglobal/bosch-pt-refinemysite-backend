/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_DAY_CARD_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class DayCardCreatedNotificationStrategy(
    private val participantService: ParticipantService,
    taskService: TaskService,
    recipientDeterminator: RecipientDeterminator
) : AbstractDayCardNotificationStrategy(taskService, recipientDeterminator) {

  override fun handles(record: EventRecord) =
      record.value is DayCardEventG2Avro &&
          (record.value as DayCardEventG2Avro).name == DayCardEventEnumAvro.CREATED

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: DayCardEventG2Avro
  ): Set<Notification> {

    val aggregate = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipients = determineRecipients(aggregate, projectIdentifier)

    return when {
      recipients.isEmpty() -> emptySet()
      else ->
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

  private fun buildSummary(
      projectIdentifier: UUID,
      event: DayCardEventG2Avro
  ): TemplateWithPlaceholders {
    val userIdentifier = event.getLastModifiedByUserIdentifier()

    val (identifier) =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = identifier,
            contextRootIdentifier = projectIdentifier)

    return TemplateWithPlaceholders(
        templateMessageKey = NOTIFICATION_SUMMARY_DAY_CARD_CREATED,
        placeholderAggregateReferenceValues = mapOf("originator" to originator))
  }

  private fun buildDetails(aggregate: DayCardAggregateG2Avro) =
      SimpleString("\"${aggregate.title}\"")
}
