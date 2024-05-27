/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.LoggerFactory

abstract class AbstractActivityStrategy<T> : ActivityStrategy {

  /**
   * This method casts the kafka record key / values to specific types required in the derived
   * classes.
   */
  @ExcludeFromCodeCoverage
  override fun apply(key: EventMessageKey, value: SpecificRecordBase?): Activity {
    LOGGER.debug("${javaClass.simpleName} handles this record")

    if (value == null) {
      return createActivityForTombstone(key)
    }

    @Suppress("UNCHECKED_CAST") val event = value as T
    return createActivity(key, event)
  }

  /** This method generates the activity in case a strategy is responsible for a certain event. */
  abstract fun createActivity(key: EventMessageKey, event: T): Activity

  open fun createActivityForTombstone(key: EventMessageKey): Activity {
    throw NotImplementedError("Unhandled tombstone message: $key")
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractActivityStrategy::class.java)
  }
}
