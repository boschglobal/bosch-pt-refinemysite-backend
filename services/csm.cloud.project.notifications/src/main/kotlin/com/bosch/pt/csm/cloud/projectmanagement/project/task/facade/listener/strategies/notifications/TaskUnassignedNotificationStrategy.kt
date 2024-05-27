/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
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
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TaskUnassignedNotificationStrategy(
    private val participantService: ParticipantService,
    private val taskService: TaskService,
    recipientDeterminator: RecipientDeterminator
) : AbstractTaskNotificationStrategy(recipientDeterminator) {

  override fun handles(record: EventRecord): Boolean =
      if (record.value !is TaskEventAvro) {
        false
      } else {
        val taskEventAvro = record.value as TaskEventAvro
        val taskEvent = taskEventAvro.name
        val taskStatus = taskEventAvro.aggregate.status

        /*
         * Strategy to validate one of the following cases:
         * - if the task was unassigned, status different than DRAFT
         */
        taskEvent == TaskEventEnumAvro.UNASSIGNED && taskStatus != TaskStatusEnumAvro.DRAFT
      }

  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TaskEventAvro
  ): Set<Notification> {

    val aggregate = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipient = determinePreviousTaskAssignee(aggregate, projectIdentifier)

    return if (recipient != null)
        setOf(
            Notification(
                notificationIdentifier = buildNotificationIdentifier(aggregate, recipient),
                event = buildEventInformation(event),
                summary = buildSummary(projectIdentifier, event),
                context = buildContext(projectIdentifier, aggregate)))
    else emptySet()
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
        templateMessageKey = Key.NOTIFICATION_SUMMARY_UNASSIGNED_TASK_FROM_YOU,
        placeholderAggregateReferenceValues = mapOf(Pair("originator", originator)))
  }

  private fun determinePreviousTaskAssignee(
      taskAggregateAvro: TaskAggregateAvro,
      projectIdentifier: UUID
  ): UUID? {
    val previousTaskAssigneeIdentifier =
        taskService.findAssigneeOfTaskWithVersion(
            taskIdentifier = taskAggregateAvro.getTaskIdentifier(),
            version = taskAggregateAvro.aggregateIdentifier.version - 1,
            projectIdentifier = projectIdentifier)
    val participant =
        participantService.findOneByIdentifierAndProjectIdentifier(
            requireNotNull(previousTaskAssigneeIdentifier), projectIdentifier)
    return if (participant.active) participant.userIdentifier else null
  }
}
