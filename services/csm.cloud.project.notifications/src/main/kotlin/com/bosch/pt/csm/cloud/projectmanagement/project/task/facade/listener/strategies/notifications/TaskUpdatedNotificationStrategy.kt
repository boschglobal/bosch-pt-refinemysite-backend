/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_WORK_AREA
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.MultipleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SingleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.AggregateComparator
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.boundary.ProjectCraftService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.boundary.WorkAreaService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
@Suppress("LongParameterList")
class TaskUpdatedNotificationStrategy(
    recipientDeterminator: RecipientDeterminator,
    private val participantService: ParticipantService,
    private val projectCraftService: ProjectCraftService,
    private val taskService: TaskService,
    private val workAreaService: WorkAreaService,
    private val aggregateComparator: AggregateComparator,
    private val notificationMerger: TaskNotificationMerger,
    private val attributeSorter: TaskAttributeSorter
) : AbstractTaskNotificationStrategy(recipientDeterminator) {

  override fun handles(record: EventRecord): Boolean {
    val key = record.key
    val value = record.value

    if (value !is TaskEventAvro || key !is AggregateEventMessageKey) return false

    val projectIdentifier = key.rootContextIdentifier
    return value.name == UPDATED &&
        value.aggregate.status != DRAFT &&
        !taskAssigneeChanged(value.aggregate, projectIdentifier)
  }

  @Trace
  override fun createNotifications(
      messageKey: EventMessageKey,
      event: TaskEventAvro
  ): Set<Notification> {

    val aggregate = event.aggregate
    val projectIdentifier = messageKey.rootContextIdentifier
    val recipients = determineRecipients(aggregate)

    return if (recipients.isEmpty()) {
      emptySet()
    } else {
      recipients
          .map {
            Notification(
                notificationIdentifier = buildNotificationIdentifier(aggregate, it),
                event = buildEventInformation(event),
                summary = buildSummary(projectIdentifier, event),
                details = buildDetails(projectIdentifier, aggregate, event, it),
                context = buildContext(projectIdentifier, aggregate))
          }
          .toSet()
    }
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
        templateMessageKey = NOTIFICATION_SUMMARY_TASK_UPDATED,
        placeholderAggregateReferenceValues = mapOf(Pair("originator", originator)))
  }

  private fun buildDetails(
      projectIdentifier: UUID,
      aggregate: TaskAggregateAvro,
      event: TaskEventAvro,
      recipientIdentifier: UUID
  ) =
      notificationMerger.mergeDetails(
          current = buildDetails(projectIdentifier, aggregate),
          previous =
              notificationMerger
                  .findMergeableNotification(recipientIdentifier, projectIdentifier, event)
                  ?.details)

  private fun buildDetails(projectIdentifier: UUID, taskAggregateAvro: TaskAggregateAvro): Details {
    val changedAttributes = differenceToPrevious(taskAggregateAvro)

    return if (changedAttributes.size == 1) {
      mapToSingleAttributeChange(changedAttributes.entries.first().toPair(), projectIdentifier)
    } else {
      MultipleAttributeChange(
          attributeSorter.sortAttributes(mapAttributeToMessageKey(changedAttributes.keys)))
    }
  }

  private fun mapToSingleAttributeChange(attribute: Pair<String, Any?>, projectIdentifier: UUID) =
      when (attribute.first) {
        "workAreaIdentifier" ->
            SingleAttributeChange(
                mapAttribute(attribute.first),
                attribute.second?.let {
                  SimpleString(
                      workAreaService.findLatest(attribute.second as UUID, projectIdentifier).name)
                })
        "craftIdentifier" ->
            SingleAttributeChange(
                mapAttribute(attribute.first),
                attribute.second?.let {
                  SimpleString(
                      projectCraftService
                          .findLatest(attribute.second as UUID, projectIdentifier)
                          .name)
                })
        else ->
            SingleAttributeChange(
                mapAttribute(attribute.first),
                attribute.second?.let { SimpleString(attribute.second.toString()) })
      }

  private fun mapAttributeToMessageKey(attributes: Set<String>) =
      attributes.map { attribute -> mapAttribute(attribute) }.toSet()

  private fun mapAttribute(attribute: String) =
      when (attribute) {
        "craftIdentifier" -> TASK_ATTRIBUTE_CRAFT
        "description" -> TASK_ATTRIBUTE_DESCRIPTION
        "location" -> TASK_ATTRIBUTE_LOCATION
        "name" -> TASK_ATTRIBUTE_NAME
        "workAreaIdentifier" -> TASK_ATTRIBUTE_WORK_AREA
        else -> error("Unknown task attribute: $attribute")
      }

  private fun differenceToPrevious(taskAggregateAvro: TaskAggregateAvro): Map<String, Any?> {
    val taskIdentifier = taskAggregateAvro.aggregateIdentifier.identifier.toUUID()
    val taskVersion = taskAggregateAvro.aggregateIdentifier.version
    val projectIdentifier = taskAggregateAvro.project.identifier.toUUID()
    val currentVersion = taskService.find(taskIdentifier, taskVersion, projectIdentifier)
    val previousVersion = taskService.find(taskIdentifier, taskVersion - 1, projectIdentifier)

    val changedAttributes = aggregateComparator.compare(currentVersion, previousVersion)

    changedAttributes.remove("assigneeIdentifier")
    changedAttributes.remove("status")

    return changedAttributes
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
