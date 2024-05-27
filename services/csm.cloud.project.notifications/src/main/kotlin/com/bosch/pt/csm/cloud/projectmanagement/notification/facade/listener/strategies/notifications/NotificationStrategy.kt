/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification

// Interface for strategies creating notifications from incoming record
interface NotificationStrategy {

  /**
   * The handle method (in general) checks, for which events a strategy is responsible (usually by
   * checking the event type). It only does further checks if there are different strategies that
   * need to be applied (either ... or) for the same event type. In that case, the handle method
   * checks the further constraints to separate the cases.*
   */
  fun handles(record: EventRecord): Boolean

  /**
   * This method generates the notifications in case a strategy is responsible for a certain event.
   * The result might also be an empty set if no recipient could be determined.
   */
  fun apply(record: EventRecord): Set<Notification>
}
