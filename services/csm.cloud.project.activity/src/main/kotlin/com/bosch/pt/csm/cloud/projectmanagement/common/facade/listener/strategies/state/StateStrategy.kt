/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import org.apache.avro.specific.SpecificRecordBase

interface StateStrategy {

  /**
   * The handle method (in general) checks, for which events a strategy is responsible (usually by
   * checking the event type). It only does further checks if there are different strategies that
   * need to be applied (either ... or) for the same event type. In that case, the handle method
   * checks the further constraints to separate the cases.
   */
  fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean

  /** This method updates the local state in case a strategy is responsible for a certain event. */
  fun apply(key: EventMessageKey, value: SpecificRecordBase?)
}
