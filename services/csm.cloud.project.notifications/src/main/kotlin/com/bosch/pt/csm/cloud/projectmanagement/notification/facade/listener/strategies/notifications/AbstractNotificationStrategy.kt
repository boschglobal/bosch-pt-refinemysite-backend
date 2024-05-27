/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import org.slf4j.LoggerFactory

abstract class AbstractNotificationStrategy<T> : NotificationStrategy {

  /**
   * This method casts the kafka record key / values to specific types required in the derived
   * classes.
   */
  @ExcludeFromCodeCoverage
  override fun apply(record: EventRecord): Set<Notification> {
    LOGGER.debug("${javaClass.simpleName} handles this record")

    val key = record.key

    if (record.value == null) {
      return createNotificationsForTombstone(key)
    }

    @Suppress("UNCHECKED_CAST") val value = record.value as T
    return createNotifications(key, value)
  }

  /**
   * This method generates the notification in case a strategy is responsible for a certain event.
   */
  abstract fun createNotifications(messageKey: EventMessageKey, event: T): Set<Notification>

  open fun createNotificationsForTombstone(messageKey: EventMessageKey): Set<Notification> {
    throw NotImplementedError("Unhandled tombstone message: $messageKey")
  }

  protected fun String.shorten(maxLength: Int): String =
      if (length > maxLength) "${substring(0, maxLength - 2)}…" else this

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractNotificationStrategy::class.java)
  }
}
