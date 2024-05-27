/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.AbstractNotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildTask
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import java.util.UUID

abstract class AbstractTaskNotificationStrategy(
    private val recipientDeterminator: RecipientDeterminator
) : AbstractNotificationStrategy<TaskEventAvro>() {

  protected fun determineRecipients(taskAggregateAvro: TaskAggregateAvro) =
      recipientDeterminator.determineDefaultRecipients(
          taskAggregateAvro.buildTask(), taskAggregateAvro.getLastModifiedByUserIdentifier())

  protected fun buildNotificationIdentifier(
      taskAggregateAvro: TaskAggregateAvro,
      recipientIdentifier: UUID
  ) = taskAggregateAvro.buildNotificationIdentifier(recipientIdentifier)

  protected fun buildEventInformation(taskEventAvro: TaskEventAvro) =
      EventInformation(
          name = taskEventAvro.name.name,
          date = taskEventAvro.getLastModifiedDate(),
          user = taskEventAvro.getLastModifiedByUserIdentifier())

  protected fun buildContext(projectIdentifier: UUID, taskAggregateAvro: TaskAggregateAvro) =
      Context(project = projectIdentifier, task = taskAggregateAvro.getTaskIdentifier())

  protected fun buildNotificationsForResetOrChangeTaskStatus(
      messageKey: EventMessageKey,
      event: TaskEventAvro,
      summary: TemplateWithPlaceholders,
      details: TemplateWithPlaceholders
  ): Set<Notification> {
    val taskAggregateAvro = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipients = determineRecipients(taskAggregateAvro)

    return if (recipients.isEmpty()) {
      emptySet()
    } else {
      recipients
          .map { recipient ->
            Notification(
                notificationIdentifier = buildNotificationIdentifier(taskAggregateAvro, recipient),
                event = buildEventInformation(event),
                summary = summary,
                details = details,
                context = buildContext(projectIdentifier, taskAggregateAvro))
          }
          .toSet()
    }
  }
}
