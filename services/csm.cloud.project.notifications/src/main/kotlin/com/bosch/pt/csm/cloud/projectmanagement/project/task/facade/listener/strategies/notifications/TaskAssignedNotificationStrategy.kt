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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_ASSIGNED_TO_YOU
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TaskAssignedNotificationStrategy(
    private val participantService: ParticipantService,
    private val taskService: TaskService,
    recipientDeterminator: RecipientDeterminator
) : AbstractTaskNotificationStrategy(recipientDeterminator) {

  override fun handles(record: EventRecord): Boolean {
    if (record.value !is TaskEventAvro) {
      return false
    } else {
      val taskEventAvro = record.value as TaskEventAvro
      val taskEvent = taskEventAvro.name
      val taskStatus = taskEventAvro.aggregate.status
      val projectIdentifier = record.key.rootContextIdentifier

      /*
       * For new events generated, only the ASSIGNED and SENT cases are relevant,
       * since the assign and send of tasks can no longer be done inside CREATED and UPDATED events.
       * The CREATED and UPDATED cases are kept only to handle old events
       *
       * Strategy to validate one of the following cases:
       * - if the task was sent and has an assignee
       * - if the task was assigned in status different than DRAFT
       * - if the task was created in status OPEN (DEPRECATED)
       * - if the task was updated in status different than DRAFT and the assignee changed (DEPRECATED)
       *   (checked last because of db-call)
       */
      return (taskEvent == SENT && taskEventAvro.getAssigneeIdentifier() != null) ||
          (taskEvent == ASSIGNED && taskStatus != DRAFT) ||
          (taskEvent == CREATED && taskStatus != DRAFT) ||
          (taskEvent == UPDATED &&
              taskStatus != DRAFT &&
              taskAssigneeChanged((record.value as TaskEventAvro).aggregate, projectIdentifier))
    }
  }

  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TaskEventAvro
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
                summary = buildSummary(projectIdentifier, event, recipient),
                context = buildContext(projectIdentifier, taskAggregateAvro))
          }
          .toSet()
    }
  }

  private fun buildSummary(
      projectIdentifier: UUID,
      taskEventAvro: TaskEventAvro,
      recipientIdentifier: UUID
  ): TemplateWithPlaceholders {
    val userIdentifier = taskEventAvro.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val assigneeParticipant =
        participantService.findOneByIdentifierAndProjectIdentifier(
            taskEventAvro.getAssigneeIdentifier()!!, projectIdentifier)

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    if (assigneeParticipant.userIdentifier == recipientIdentifier) {
      return TemplateWithPlaceholders(
          templateMessageKey = NOTIFICATION_SUMMARY_TASK_ASSIGNED_TO_YOU,
          placeholderAggregateReferenceValues = mapOf(Pair("originator", originator)))
    } else {
      val assignee =
          ObjectReferenceWithContextRoot(
              type = "PARTICIPANT",
              identifier = assigneeParticipant.identifier,
              contextRootIdentifier = projectIdentifier)

      return TemplateWithPlaceholders(
          templateMessageKey = NOTIFICATION_SUMMARY_TASK_ASSIGNED,
          placeholderAggregateReferenceValues =
              mapOf(Pair("originator", originator), Pair("assignee", assignee)))
    }
  }

  private fun taskAssigneeChanged(
      taskAggregateAvro: TaskAggregateAvro,
      projectIdentifier: UUID
  ): Boolean {
    val previousTaskAssignee =
        taskService.findAssigneeOfTaskWithVersion(
            taskIdentifier = taskAggregateAvro.getTaskIdentifier(),
            version = taskAggregateAvro.aggregateIdentifier.version - 1,
            projectIdentifier = projectIdentifier)
    return taskAggregateAvro.getAssigneeIdentifier() != previousTaskAssignee
  }
}
