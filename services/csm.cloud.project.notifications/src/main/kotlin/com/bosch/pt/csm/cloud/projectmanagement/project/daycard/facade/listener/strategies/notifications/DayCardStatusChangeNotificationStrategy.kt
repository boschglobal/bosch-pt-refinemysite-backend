/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RFVCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_BADWEATHER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_CHANGEDPRIORITY
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_CONCESSIONNOTRECOGNIZED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_DELAYEDMATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_MANPOWERSHORTAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_MISSINGINFOS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_MISSINGTOOLS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_NOCONCESSION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_OVERESTIMATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_REASON_ENUM_TOUCHUP
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_STATUS_ENUM_APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_STATUS_ENUM_DONE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_STATUS_ENUM_NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_STATUS_ENUM_OPEN
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_DETAILS_DAY_CARD_STATUS_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_DETAILS_DAY_CARD_STATUS_UPDATED_WITH_REASON
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_DAY_CARD_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithValuePlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.COMPLETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.BAD_WEATHER
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CHANGED_PRIORITY
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CONCESSION_NOT_RECOGNIZED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM3
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM4
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.DELAYED_MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MANPOWER_SHORTAGE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MISSING_INFOS
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MISSING_TOOLS
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.NO_CONCESSION
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.OVERESTIMATION
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.TOUCHUP
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class DayCardStatusChangeNotificationStrategy(
    private val participantService: ParticipantService,
    taskService: TaskService,
    recipientDeterminator: RecipientDeterminator
) : AbstractDayCardNotificationStrategy(taskService, recipientDeterminator) {

  override fun handles(record: EventRecord) =
      record.value is DayCardEventG2Avro &&
          ((record.value as DayCardEventG2Avro).name == CANCELLED ||
              (record.value as DayCardEventG2Avro).name == APPROVED ||
              (record.value as DayCardEventG2Avro).name == COMPLETED ||
              (record.value as DayCardEventG2Avro).name == RESET)

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
                    details = buildDetails(event),
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

    val actorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = actorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return TemplateWithPlaceholders(
        templateMessageKey = NOTIFICATION_SUMMARY_DAY_CARD_UPDATED,
        placeholderAggregateReferenceValues = mapOf("originator" to originator))
  }

  private fun buildDetails(event: DayCardEventG2Avro) =
      when (CANCELLED) {
        event.getName() -> buildDetailsForCancellationEvent(event)
        else -> buildDetailsForOtherEvents(event)
      }

  private fun buildDetailsForCancellationEvent(event: DayCardEventG2Avro) =
      TemplateWithValuePlaceholders(
          templateMessageKey = NOTIFICATION_DETAILS_DAY_CARD_STATUS_UPDATED_WITH_REASON,
          placeholderValues =
              mapOf(
                  "status" to getStatusDetails(event.aggregate.status),
                  "reason" to getReasonDetails(event.aggregate.reason)))

  private fun buildDetailsForOtherEvents(event: DayCardEventG2Avro) =
      TemplateWithValuePlaceholders(
          templateMessageKey = NOTIFICATION_DETAILS_DAY_CARD_STATUS_UPDATED,
          placeholderValues = mapOf("status" to getStatusDetails(event.aggregate.status)))

  private fun getStatusDetails(status: DayCardStatusEnumAvro) =
      when (status) {
        DayCardStatusEnumAvro.APPROVED -> SimpleMessageKey(DAY_CARD_STATUS_ENUM_APPROVED)
        DayCardStatusEnumAvro.DONE -> SimpleMessageKey(DAY_CARD_STATUS_ENUM_DONE)
        DayCardStatusEnumAvro.NOTDONE -> SimpleMessageKey(DAY_CARD_STATUS_ENUM_NOTDONE)
        DayCardStatusEnumAvro.OPEN -> SimpleMessageKey(DAY_CARD_STATUS_ENUM_OPEN)
        else -> error("Unknown dayCard status received.")
      }

  @ExcludeFromCodeCoverage
  private fun getReasonDetails(notDoneReason: DayCardReasonNotDoneEnumAvro) =
      when (notDoneReason) {
        BAD_WEATHER -> SimpleMessageKey(DAY_CARD_REASON_ENUM_BADWEATHER)
        CHANGED_PRIORITY -> SimpleMessageKey(DAY_CARD_REASON_ENUM_CHANGEDPRIORITY)
        CONCESSION_NOT_RECOGNIZED -> SimpleMessageKey(DAY_CARD_REASON_ENUM_CONCESSIONNOTRECOGNIZED)
        DELAYED_MATERIAL -> SimpleMessageKey(DAY_CARD_REASON_ENUM_DELAYEDMATERIAL)
        MANPOWER_SHORTAGE -> SimpleMessageKey(DAY_CARD_REASON_ENUM_MANPOWERSHORTAGE)
        MISSING_INFOS -> SimpleMessageKey(DAY_CARD_REASON_ENUM_MISSINGINFOS)
        MISSING_TOOLS -> SimpleMessageKey(DAY_CARD_REASON_ENUM_MISSINGTOOLS)
        NO_CONCESSION -> SimpleMessageKey(DAY_CARD_REASON_ENUM_NOCONCESSION)
        OVERESTIMATION -> SimpleMessageKey(DAY_CARD_REASON_ENUM_OVERESTIMATION)
        TOUCHUP -> SimpleMessageKey(DAY_CARD_REASON_ENUM_TOUCHUP)
        CUSTOM1 -> LazyValue(CUSTOM1, RFVCUSTOMIZATION.name)
        CUSTOM2 -> LazyValue(CUSTOM2, RFVCUSTOMIZATION.name)
        CUSTOM3 -> LazyValue(CUSTOM3, RFVCUSTOMIZATION.name)
        CUSTOM4 -> LazyValue(CUSTOM4, RFVCUSTOMIZATION.name)
        else -> error("Unknown dayCard not done reason received.")
      }
}
