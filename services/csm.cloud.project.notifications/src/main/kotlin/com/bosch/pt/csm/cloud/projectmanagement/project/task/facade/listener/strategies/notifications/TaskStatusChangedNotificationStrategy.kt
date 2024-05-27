/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TaskStatusChangedNotificationStrategy(
    private val participantService: ParticipantService,
    recipientDeterminator: RecipientDeterminator
) : AbstractTaskNotificationStrategy(recipientDeterminator) {

  override fun handles(record: EventRecord) =
      record.value is TaskEventAvro &&
          (record.value as TaskEventAvro).name in
              arrayOf(
                  TaskEventEnumAvro.STARTED, TaskEventEnumAvro.CLOSED, TaskEventEnumAvro.ACCEPTED)

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TaskEventAvro
  ): Set<Notification> {

    val projectIdentifier = messageKey.rootContextIdentifier
    return buildNotificationsForResetOrChangeTaskStatus(
        messageKey, event, buildSummary(projectIdentifier, event), buildDetails(event.aggregate))
  }

  private fun buildSummary(
      projectIdentifier: UUID,
      taskEventAvro: TaskEventAvro
  ): TemplateWithPlaceholders {
    val userIdentifier = taskEventAvro.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return TemplateWithPlaceholders(
        templateMessageKey = Key.NOTIFICATION_SUMMARY_TASK_UPDATED,
        placeholderAggregateReferenceValues = mapOf(Pair("originator", originator)))
  }

  private fun buildDetails(taskAggregateAvro: TaskAggregateAvro): TemplateWithPlaceholders {
    return TemplateWithPlaceholders(
        templateMessageKey = Key.NOTIFICATION_DETAILS_TASK_STATUS_UPDATED,
        placeholderMessageKeyValues =
            mapOf(Pair("status", getMessageKeyForTaskStatus(taskAggregateAvro.status))))
  }

  private fun getMessageKeyForTaskStatus(taskStatus: TaskStatusEnumAvro): String {
    return when (taskStatus) {
      TaskStatusEnumAvro.STARTED -> Key.TASK_STATUS_ENUM_STARTED
      TaskStatusEnumAvro.CLOSED -> Key.TASK_STATUS_ENUM_CLOSED
      TaskStatusEnumAvro.ACCEPTED -> Key.TASK_STATUS_ENUM_ACCEPTED
      else -> error("Unknown task status received.")
    }
  }
}
