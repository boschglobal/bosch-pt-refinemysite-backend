/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.LoggerFactory

abstract class AbstractStateStrategy<T> : StateStrategy {

  /**
   * This method casts the kafka record key / values to specific types required in the derived
   * classes.
   */
  @ExcludeFromCodeCoverage
  override fun apply(key: EventMessageKey, value: SpecificRecordBase?) {
    LOGGER.debug("${javaClass.simpleName} handles this record")

    if (value == null) {
      return updateStateForTombstone(key)
    }

    @Suppress("UNCHECKED_CAST") val event = value as T
    updateState(key, event)
  }

  /** This method generates the activity in case a strategy is responsible for a certain event. */
  open fun updateState(key: EventMessageKey, event: T): Unit =
      throw NotImplementedError("Unhandled event: $event")

  open fun updateStateForTombstone(key: EventMessageKey): Unit =
      throw NotImplementedError("Unhandled tombstone message: $key")

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractStateStrategy::class.java)
  }
}
