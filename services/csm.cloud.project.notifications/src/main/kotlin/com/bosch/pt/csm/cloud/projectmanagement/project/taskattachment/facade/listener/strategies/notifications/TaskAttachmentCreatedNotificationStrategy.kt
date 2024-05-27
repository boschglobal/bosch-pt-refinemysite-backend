/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.AbstractNotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.CountableAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications.TaskNotificationMerger
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getTaskVersion
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TaskAttachmentCreatedNotificationStrategy(
    private val recipientDeterminator: RecipientDeterminator,
    private val participantService: ParticipantService,
    private val taskService: TaskService,
    private val notificationService: NotificationService,
    private val notificationMerger: TaskNotificationMerger
) : AbstractNotificationStrategy<TaskAttachmentEventAvro>() {

  override fun handles(record: EventRecord) =
      if (record.value !is TaskAttachmentEventAvro) {
        false
      } else {
        val taskAttachmentEvent = record.value as TaskAttachmentEventAvro
        val task =
            taskService.find(
                taskIdentifier = taskAttachmentEvent.getTaskIdentifier(),
                version = taskAttachmentEvent.getTaskVersion(),
                projectIdentifier = record.key.rootContextIdentifier)
        taskAttachmentEvent.name == TaskAttachmentEventEnumAvro.CREATED &&
            task.status != TaskStatusEnum.DRAFT
      }

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TaskAttachmentEventAvro
  ): Set<Notification> {

    val aggregate = event.getAggregate()
    val projectIdentifier = messageKey.rootContextIdentifier

    val task =
        taskService.find(
            taskIdentifier = aggregate.getTaskIdentifier(),
            version = aggregate.getTaskVersion(),
            projectIdentifier = projectIdentifier)

    val recipients =
        recipientDeterminator.determineDefaultRecipients(
            task = task, lastModifiedByUserIdentifier = aggregate.getLastModifiedByUserIdentifier())

    return if (recipients.isEmpty()) {
      emptySet()
    } else {
      // in case we have duplicated events in our topic, we need to skip processing those
      // for which notification have already been created.
      if (notificationService.findOneById(
          aggregate.buildNotificationIdentifier(recipients.first())) != null) {
        return emptySet()
      }
      recipients
          .map {
            Notification(
                notificationIdentifier = buildNotificationIdentifier(aggregate, it),
                event = buildEventInformation(event),
                summary = buildSummary(projectIdentifier, event),
                details = buildDetails(projectIdentifier, event, it),
                context = buildContext(projectIdentifier, aggregate))
          }
          .toSet()
    }
  }

  private fun buildNotificationIdentifier(
      aggregate: TaskAttachmentAggregateAvro,
      recipientIdentifier: UUID
  ) = aggregate.buildNotificationIdentifier(recipientIdentifier)

  private fun buildSummary(
      projectIdentifier: UUID,
      taskAttachmentEventAvro: TaskAttachmentEventAvro
  ): TemplateWithPlaceholders {
    val userIdentifier = taskAttachmentEventAvro.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
            projectIdentifier, userIdentifier)!!

    val originator =
        ObjectReferenceWithContextRoot(
            type = "PARTICIPANT",
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return TemplateWithPlaceholders(
        templateMessageKey = NOTIFICATION_SUMMARY_TASK_UPDATED,
        placeholderAggregateReferenceValues = mapOf("originator" to originator))
  }

  private fun buildDetails(
      projectIdentifier: UUID,
      event: TaskAttachmentEventAvro,
      recipientIdentifier: UUID
  ) =
      notificationMerger.mergeDetails(
          current = buildDetails(),
          previous =
              notificationMerger
                  .findMergeableNotification(recipientIdentifier, projectIdentifier, event)
                  ?.details)

  private fun buildDetails() =
      CountableAttributeChange(
          NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE, TASK_ATTRIBUTE_ATTACHMENT, 1)

  private fun buildEventInformation(event: TaskAttachmentEventAvro) =
      EventInformation(
          name = event.getName().name,
          date = event.getLastModifiedDate(),
          user = event.getLastModifiedByUserIdentifier())

  private fun buildContext(projectIdentifier: UUID, aggregate: TaskAttachmentAggregateAvro) =
      Context(project = projectIdentifier, task = aggregate.getTaskIdentifier())
}
