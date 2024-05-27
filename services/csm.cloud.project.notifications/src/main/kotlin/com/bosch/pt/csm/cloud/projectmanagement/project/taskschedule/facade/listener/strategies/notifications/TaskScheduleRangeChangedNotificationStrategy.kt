/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.firstKey
import com.bosch.pt.csm.cloud.common.extensions.firstValue
import com.bosch.pt.csm.cloud.common.extensions.toLocalDate
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_START
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.AbstractNotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.MultipleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleDate
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SingleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.AggregateComparator
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications.TaskNotificationMerger
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.boundary.TaskScheduleService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskVersion
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getVersion
import datadog.trace.api.Trace
import java.util.Date
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TaskScheduleRangeChangedNotificationStrategy(
    private val taskService: TaskService,
    private val scheduleService: TaskScheduleService,
    private val participantService: ParticipantService,
    private val recipientDeterminator: RecipientDeterminator,
    private val aggregateComparator: AggregateComparator,
    private val notificationMerger: TaskNotificationMerger
) : AbstractNotificationStrategy<TaskScheduleEventAvro>() {

  override fun handles(record: EventRecord): Boolean {
    if (record.value !is TaskScheduleEventAvro) return false

    val scheduleEvent = record.value as TaskScheduleEventAvro
    val projectIdentifier = record.key.rootContextIdentifier

    if (taskService
        .find(scheduleEvent.getTaskIdentifier(), scheduleEvent.getTaskVersion(), projectIdentifier)
        .status == TaskStatusEnum.DRAFT)
        return false

    return scheduleEvent.name in
        arrayOf(TaskScheduleEventEnumAvro.CREATED, TaskScheduleEventEnumAvro.DELETED) ||
        startOrEndChanged(scheduleEvent.aggregate, projectIdentifier, scheduleEvent.name)
  }

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TaskScheduleEventAvro
  ): Set<Notification> {

    val aggregate = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipients =
        recipientDeterminator.determineDefaultRecipients(
            taskService.findLatest(event.getTaskIdentifier(), projectIdentifier),
            aggregate.getLastModifiedByUserIdentifier())

    return if (recipients.isEmpty()) {
      emptySet()
    } else {
      recipients
          .map {
            Notification(
                notificationIdentifier = buildNotificationIdentifier(aggregate, it),
                event = buildEventInformation(event),
                summary = buildSummary(projectIdentifier, aggregate),
                details = buildDetails(projectIdentifier, aggregate, event, it),
                context = buildContext(projectIdentifier, aggregate))
          }
          .toSet()
    }
  }

  private fun startOrEndChanged(
      schedule: TaskScheduleAggregateAvro,
      projectIdentifier: UUID,
      event: TaskScheduleEventEnumAvro
  ) = differenceToPrevious(schedule, projectIdentifier, event).isNotEmpty()

  private fun buildNotificationIdentifier(aggregate: TaskScheduleAggregateAvro, recipient: UUID) =
      aggregate.buildNotificationIdentifier(recipient)

  private fun buildEventInformation(event: TaskScheduleEventAvro) =
      EventInformation(
          name = event.name.name,
          date = event.getLastModifiedDate(),
          user = event.getLastModifiedByUserIdentifier())

  private fun buildSummary(projectIdentifier: UUID, aggregate: TaskScheduleAggregateAvro) =
      TemplateWithPlaceholders(
          templateMessageKey = NOTIFICATION_SUMMARY_TASK_UPDATED,
          placeholderAggregateReferenceValues =
              mapOf(
                  "originator" to
                      ObjectReferenceWithContextRoot(
                          type = "PARTICIPANT",
                          identifier =
                              participantService
                                  .findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
                                      projectIdentifier = projectIdentifier,
                                      userIdentifier =
                                          aggregate.getLastModifiedByUserIdentifier())!!
                                  .identifier,
                          contextRootIdentifier = projectIdentifier)))

  private fun buildDetails(
      projectIdentifier: UUID,
      aggregate: TaskScheduleAggregateAvro,
      event: TaskScheduleEventAvro,
      recipientIdentifier: UUID
  ) =
      notificationMerger.mergeDetails(
          current = buildDetails(projectIdentifier, aggregate, event.name),
          previous =
              notificationMerger
                  .findMergeableNotification(recipientIdentifier, projectIdentifier, event)
                  ?.details)

  private fun buildDetails(
      projectIdentifier: UUID,
      aggregate: TaskScheduleAggregateAvro,
      event: TaskScheduleEventEnumAvro
  ): Details {
    val changedAttributes = differenceToPrevious(aggregate, projectIdentifier, event)

    return if (changedAttributes.size == 1) {
      val attribute =
          when (changedAttributes.firstKey()) {
            "start" -> TASK_SCHEDULE_ATTRIBUTE_START
            "end" -> TASK_SCHEDULE_ATTRIBUTE_END
            else -> error("Unknown schedule attribute: ${changedAttributes.firstKey()}")
          }

      SingleAttributeChange(
          attribute, changedAttributes.firstValue()?.let { SimpleDate((it as Date).toLocalDate()) })
    } else {
      MultipleAttributeChange(listOf(TASK_SCHEDULE_ATTRIBUTE_START, TASK_SCHEDULE_ATTRIBUTE_END))
    }
  }

  private fun buildContext(projectIdentifier: UUID, aggregate: TaskScheduleAggregateAvro) =
      Context(projectIdentifier, aggregate.getTaskIdentifier())

  private fun differenceToPrevious(
      schedule: TaskScheduleAggregateAvro,
      projectIdentifier: UUID,
      event: TaskScheduleEventEnumAvro
  ): Map<String, Any?> {
    val currentVersion =
        if (event == TaskScheduleEventEnumAvro.DELETED) null
        else
            scheduleService.find(schedule.getIdentifier(), schedule.getVersion(), projectIdentifier)
    val previousVersion =
        scheduleService.find(schedule.getIdentifier(), schedule.getVersion() - 1, projectIdentifier)
    val changedAttributes = aggregateComparator.compare(currentVersion, previousVersion)

    return changedAttributes.apply {
      remove("slots")
      remove("_class")
      remove("projectIdentifier")
      remove("_id.identifier")
      remove("_id.type")
      remove("taskIdentifier")
    }
  }
}
