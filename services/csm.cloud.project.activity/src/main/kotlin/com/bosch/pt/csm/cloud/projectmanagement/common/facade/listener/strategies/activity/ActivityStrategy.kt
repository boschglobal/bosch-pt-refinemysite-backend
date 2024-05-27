/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import org.apache.avro.specific.SpecificRecordBase

interface ActivityStrategy {

  /**
   * The handle method (in general) checks, for which events a strategy is responsible (usually by
   * checking the event type). It only does further checks if there are different strategies that
   * need to be applied (either ... or) for the same event type. In that case, the handle method
   * checks the further constraints to separate the cases.
   */
  fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean

  /** Creates the activity for the given event. */
  fun apply(key: EventMessageKey, value: SpecificRecordBase?): Activity
}
