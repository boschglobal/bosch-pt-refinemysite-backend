/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ATTRIBUTE_MANPOWER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ATTRIBUTE_NOTES
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ATTRIBUTE_TITLE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_DAY_CARD_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getDayCardVersion
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.MultipleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SingleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.AggregateComparator
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.boundary.DayCardService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class DayCardUpdateNotificationStrategy(
    private val participantService: ParticipantService,
    private val dayCardService: DayCardService,
    private val aggregateComparator: AggregateComparator,
    private val attributeSorter: DayCardAttributeSorter,
    taskService: TaskService,
    recipientDeterminator: RecipientDeterminator
) : AbstractDayCardNotificationStrategy(taskService, recipientDeterminator) {

  override fun handles(record: EventRecord) =
      record.value is DayCardEventG2Avro &&
          (record.value as DayCardEventG2Avro).name == DayCardEventEnumAvro.UPDATED

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: DayCardEventG2Avro
  ): Set<Notification> {

    val aggregate = event.getAggregate()
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
                    details = buildDetails(projectIdentifier, aggregate),
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
        templateMessageKey = NOTIFICATION_SUMMARY_DAY_CARD_UPDATED,
        placeholderAggregateReferenceValues = mapOf("originator" to originator))
  }

  private fun buildDetails(projectIdentifier: UUID, aggregate: DayCardAggregateG2Avro): Details {
    val changedAttributes = differenceToPrevious(projectIdentifier, aggregate)

    return when (changedAttributes.size) {
      1 -> {
        val attribute = changedAttributes.entries.first().toPair()

        SingleAttributeChange(
          mapAttribute(attribute.first),
          attribute.second?.let { SimpleString(attribute.second.toString()) })
      }

      else -> {
        val attributes = changedAttributes.keys.map { attribute -> mapAttribute(attribute) }.toSet()

        MultipleAttributeChange(attributeSorter.sortAttributes(attributes))
      }
    }
  }

  private fun differenceToPrevious(
      projectIdentifier: UUID,
      aggregate: DayCardAggregateG2Avro
  ): Map<String, Any?> {
    val dayCardIdentifier = aggregate.getIdentifier()
    val dayCardVersion = aggregate.getDayCardVersion()
    val currentVersion = dayCardService.find(dayCardIdentifier, dayCardVersion, projectIdentifier)
    val previousVersion =
        dayCardService.find(dayCardIdentifier, dayCardVersion - 1, projectIdentifier)

    return aggregateComparator.compare(currentVersion, previousVersion)
  }

  @ExcludeFromCodeCoverage
  private fun mapAttribute(attribute: String) =
      when (attribute) {
        "title" -> DAY_CARD_ATTRIBUTE_TITLE
        "manpower" -> DAY_CARD_ATTRIBUTE_MANPOWER
        "notes" -> DAY_CARD_ATTRIBUTE_NOTES
        else -> error("Unknown day card attribute: $attribute")
      }
}
