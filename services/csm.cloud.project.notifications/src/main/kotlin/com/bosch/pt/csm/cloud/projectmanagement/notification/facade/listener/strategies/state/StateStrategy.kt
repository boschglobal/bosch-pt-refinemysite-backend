/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord

/** Interface for strategies modifying the local state from incoming records. */
interface StateStrategy {

  /**
   * The handle method (in general) checks, for which events a strategy is responsible (usually by
   * checking the event type). It only does further checks if there are different strategies that
   * need to be applied (either ... or) for the same event type. In that case, the handle method
   * checks the further constraints to separate the cases.*
   */
  fun handles(record: EventRecord): Boolean

  /** This method updates the local state in case a strategy is responsible for a certain event. */
  fun apply(record: EventRecord)
}
