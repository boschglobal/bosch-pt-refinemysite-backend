/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.BusinessTransactionAware
import com.bosch.pt.csm.cloud.projectmanagement.application.config.ProcessStateOnlyProperties
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.event.boundary.EventService
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.BaseEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.NotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import org.springframework.stereotype.Component

@Component
class ProjectEventProcessor(
    updateStateStrategies: Set<UpdateStateStrategy>,
    cleanUpStateStrategies: Set<CleanUpStateStrategy>,
    processStateOnlyProperties: ProcessStateOnlyProperties,
    private val notificationStrategies: Set<NotificationStrategy>,
    private val notificationService: NotificationService,
    private val eventService: EventService,
) :
    BaseEventProcessor(updateStateStrategies, cleanUpStateStrategies, processStateOnlyProperties),
    BusinessTransactionAware {

  override fun getProcessorName(): String = "notifications"

  override fun onNonTransactionalEvent(record: EventRecord) {
    processSingleEvent(record, !shouldProcessStateOnly(record))
  }

  override fun onTransactionFinished(
      transactionStartedRecord: EventRecord,
      events: List<EventRecord>,
      transactionFinishedRecord: EventRecord
  ) {
    events.forEach { event ->
      processSingleEvent(
          event,
          transactionStartedRecord.value !is ProjectCopyStartedEventAvro &&
              !shouldProcessStateOnly(event))
    }
  }

  private fun processSingleEvent(record: EventRecord, sendNotifications: Boolean) {
    updateState(record)
    if (sendNotifications) {
      val notifications = createNotifications(record)
      if (notifications.isNotEmpty()) {
        notificationService.saveAll(notifications)
        notifyUsers(notifications)
      }
    }
    cleanUpState(record)
  }

  private fun createNotifications(record: EventRecord) =
      notificationStrategies
          .filter { it.handles(record) }
          .map { it.apply(record) }
          .flatten()
          .toSet()

  private fun notifyUsers(notifications: Set<Notification>) {
    notifications.forEach { notification ->
      eventService.send(
          notification.notificationIdentifier.recipientIdentifier, notification.insertDate)
    }
  }
}
