/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.boundary

import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.dto.GenericPageListWrapper
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.NotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.NotificationRepository
import datadog.trace.api.Trace
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.IdGenerator

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val idGenerator: IdGenerator
) {
  @Trace
  fun markAsRead(userIdentifier: UUID, externalIdentifier: UUID) =
      notificationRepository.markAsRead(userIdentifier, externalIdentifier)

  @Trace
  fun markAsMerged(userIdentifier: UUID, externalIdentifier: UUID) =
      notificationRepository.markAsMerged(userIdentifier, externalIdentifier)

  @Trace
  fun saveAll(notifications: Set<Notification>) =
      notifications.forEach { notification -> save(notification) }

  @Trace
  fun findAll(userIdentifier: UUID, limit: Int?) =
      wrap(
          notificationRepository.findAll(userIdentifier, searchLimit(limit)).toMutableList(),
          searchLimit(limit))

  @Trace
  fun findAllBefore(userIdentifier: UUID, before: Instant, limit: Int?) =
      wrap(
          notificationRepository
              .findAllBefore(userIdentifier, before, searchLimit(limit))
              .toMutableList(),
          searchLimit(limit))

  @Trace
  fun findAllAfter(userIdentifier: UUID, after: Instant, limit: Int?) =
      wrap(
          notificationRepository
              .findAllAfter(userIdentifier, after, searchLimit(limit))
              .toMutableList(),
          searchLimit(limit))

  @Trace
  fun findOneByExternalIdentifier(userIdentifier: UUID, externalIdentifier: UUID) =
      notificationRepository
          .findOneByNotificationIdentifierRecipientIdentifierAndExternalIdentifier(
              userIdentifier, externalIdentifier)

  @Trace
  fun findOneById(notificationIdentifier: NotificationIdentifier): Notification? =
      notificationRepository.findById(notificationIdentifier).orElse(null)

  @Trace
  fun deleteNotifications(userIdentifier: UUID, projectIdentifier: UUID) =
      notificationRepository.deleteNotifications(userIdentifier, projectIdentifier)

  @Trace
  @Suppress("LongParameterList")
  fun findMergeableTaskUpdatedNotification(
      recipient: UUID,
      projectIdentifier: UUID,
      taskIdentifier: UUID,
      summaryMessageKey: String,
      eventDate: Instant,
      eventUser: UUID
  ) =
      notificationRepository.findMergeableTaskUpdatedNotification(
          recipient, projectIdentifier, taskIdentifier, summaryMessageKey, eventDate, eventUser)

  @Trace
  private fun save(notification: Notification) {
    notification.externalIdentifier = notification.externalIdentifier ?: idGenerator.generateId()
    notification.insertDate = Instant.now()

    // idempotency: if the notification already exists due to (re-)processing of the same (or
    // a duplicated) message, make sure to keep the externalIdentifier and insertDate from the
    // original notification.
    notificationRepository.findById(notification.notificationIdentifier).ifPresent {
        existingNotification ->
      LOGGER.debug("Notification already exists. Updating existing notification.")
      notification.externalIdentifier = existingNotification.externalIdentifier
      notification.insertDate = existingNotification.insertDate
    }

    notificationRepository.save(notification)

    LOGGER.debug(
        "Notification saved for user {} date {} and with read status {}",
        notification.notificationIdentifier.recipientIdentifier,
        notification.event.date,
        notification.read)
  }

  private fun wrap(
      notifications: MutableList<Notification>,
      searchLimit: Int
  ): GenericPageListWrapper<Notification> {
    if (notifications.size > searchLimit && searchLimit > 0) {
      // If the last x  notifications have the same date, remove them all
      for (i in notifications.size - 1 downTo 2) {
        if (notifications[i].event.date == notifications[i - 1].event.date) {
          notifications.removeAt(i)
        } else {
          break
        }
      }
      if (notifications.size > searchLimit) {
        for (i in notifications.size - 1 downTo searchLimit) {
          notifications.removeAt(i)
        }
      }
      return GenericPageListWrapper(resources = notifications, previous = true)
    }
    return GenericPageListWrapper(resources = notifications, previous = false)
  }

  private fun searchLimit(limit: Int?) =
      if (limit == null || limit > MAX_NOTIFICATIONS) MAX_NOTIFICATIONS else limit

  companion object {
    const val MAX_NOTIFICATIONS = 50
    private val LOGGER = LoggerFactory.getLogger(NotificationService::class.java)
  }
}
