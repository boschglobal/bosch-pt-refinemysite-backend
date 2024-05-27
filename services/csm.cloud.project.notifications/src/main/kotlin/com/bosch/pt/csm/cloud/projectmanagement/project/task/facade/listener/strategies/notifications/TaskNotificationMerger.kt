/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.extensions.toList
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.CountableAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.MultipleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SingleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskIdentifier
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TaskNotificationMerger(
    private val notificationService: NotificationService,
    private val attributeSorter: TaskAttributeSorter
) {

  fun findMergeableNotification(
      recipient: UUID,
      projectIdentifier: UUID,
      event: TaskAttachmentEventAvro
  ) =
      notificationService
          .findMergeableTaskUpdatedNotification(
              recipient = recipient,
              projectIdentifier = projectIdentifier,
              taskIdentifier = event.getTaskIdentifier(),
              summaryMessageKey = Key.NOTIFICATION_SUMMARY_TASK_UPDATED,
              eventUser = event.getLastModifiedByUserIdentifier(),
              eventDate = event.getLastModifiedDate().minusSeconds(LOOK_BACK_SECONDS))
          ?.apply {
            notificationService.markAsMerged(
                userIdentifier = notificationIdentifier.recipientIdentifier,
                externalIdentifier = externalIdentifier!!)
          }

  fun findMergeableNotification(recipient: UUID, projectIdentifier: UUID, event: TaskEventAvro) =
      notificationService
          .findMergeableTaskUpdatedNotification(
              recipient = recipient,
              projectIdentifier = projectIdentifier,
              taskIdentifier = event.getTaskIdentifier(),
              summaryMessageKey = Key.NOTIFICATION_SUMMARY_TASK_UPDATED,
              eventUser = event.getLastModifiedByUserIdentifier(),
              eventDate = event.getLastModifiedDate().minusSeconds(LOOK_BACK_SECONDS))
          ?.apply {
            notificationService.markAsMerged(
                userIdentifier = notificationIdentifier.recipientIdentifier,
                externalIdentifier = externalIdentifier!!)
          }

  fun findMergeableNotification(
      recipient: UUID,
      projectIdentifier: UUID,
      event: TaskScheduleEventAvro
  ) =
      notificationService
          .findMergeableTaskUpdatedNotification(
              recipient = recipient,
              projectIdentifier = projectIdentifier,
              taskIdentifier = event.getTaskIdentifier(),
              summaryMessageKey = Key.NOTIFICATION_SUMMARY_TASK_UPDATED,
              eventUser = event.getLastModifiedByUserIdentifier(),
              eventDate = event.getLastModifiedDate().minusSeconds(LOOK_BACK_SECONDS))
          ?.apply {
            notificationService.markAsMerged(
                userIdentifier = notificationIdentifier.recipientIdentifier,
                externalIdentifier = externalIdentifier!!)
          }

  fun mergeDetails(current: Details, previous: Details?) =
      if (previous == null) {
        current
      } else {
        when (current) {
          is SingleAttributeChange ->
              mergeToMultipleAttributeChange(current.attribute.toList(), previous)
          is MultipleAttributeChange -> mergeToMultipleAttributeChange(current.attributes, previous)
          is CountableAttributeChange -> mergeToCountableAttributeChange(current, previous)
          else -> error("Current notification has unsupported details type")
        }
      }

  private fun mergeToCountableAttributeChange(
      current: CountableAttributeChange,
      previous: Details
  ) =
      if (previous is CountableAttributeChange) {
        mergeCountableAttributeChanges(current, previous)
      } else {
        mergeToMultipleAttributeChange(current.attribute.toList(), previous)
      }

  private fun mergeCountableAttributeChanges(
      current: CountableAttributeChange,
      previous: CountableAttributeChange
  ) =
      if (current.attribute == Key.TASK_ATTRIBUTE_ATTACHMENT &&
          previous.attribute in
              setOf(Key.TASK_ATTRIBUTE_ATTACHMENT, Key.TASK_ATTRIBUTE_ATTACHMENTS)) {
        CountableAttributeChange(
            Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_MULTIPLE,
            Key.TASK_ATTRIBUTE_ATTACHMENTS,
            previous.value + 1)
      } else {
        error("Unsupported countable attribute changes were tried to be merged.")
      }

  private fun mergeToMultipleAttributeChange(current: List<String>, previous: Details) =
      when (previous) {
        is SingleAttributeChange -> multipleAttributeChange(current, previous.attribute.toList())
        is MultipleAttributeChange -> multipleAttributeChange(current, previous.attributes)
        is CountableAttributeChange -> multipleAttributeChange(current, previous.attribute.toList())
        else -> error("Mergeable notification has unsupported details type")
      }

  private fun multipleAttributeChange(current: List<String>, previous: List<String>) =
      MultipleAttributeChange(
          attributes = attributeSorter.sortAttributes(mergeAttributes(current, previous)))

  private fun mergeAttributes(current: List<String>, previous: List<String>): Set<String> {
    val combinedList = previous.toMutableList()
    current
        .filter { !combinedList.contains(pluralOf(it)) }
        .forEach {
          if (combinedList.contains(it)) {
            combinedList
                .apply {
                  remove(it)
                  add(pluralOf(it))
                }
                .toSet()
          } else {
            combinedList.add(it)
          }
        }
    return combinedList.toSet()
  }

  private fun pluralOf(singular: String) =
      when (singular) {
        Key.TASK_ATTRIBUTE_ATTACHMENT -> Key.TASK_ATTRIBUTE_ATTACHMENTS
        else -> singular
      }

  companion object {
    const val LOOK_BACK_SECONDS: Long = 8
  }
}
