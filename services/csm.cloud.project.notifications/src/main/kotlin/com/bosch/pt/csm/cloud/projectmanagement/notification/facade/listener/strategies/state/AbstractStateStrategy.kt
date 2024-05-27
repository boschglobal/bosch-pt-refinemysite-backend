/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import org.slf4j.LoggerFactory

abstract class AbstractStateStrategy<T> : StateStrategy {

  /**
   * This method casts the kafka record key / values to specific types required in the derived
   * classes.
   */
  @ExcludeFromCodeCoverage
  override fun apply(record: EventRecord) {
    LOGGER.debug("${javaClass.simpleName} handles this record")

    val key = record.key

    if (record.value == null) {
      @Suppress("UNREACHABLE_CODE") return updateStateForTombstone(key)
    }

    @Suppress("UNCHECKED_CAST") val value = record.value as T
    updateState(key, value)
  }

  /** This method generates the activity in case a strategy is responsible for a certain event. */
  open fun updateState(messageKey: EventMessageKey, event: T): Unit =
      throw NotImplementedError("Unhandled event: $event")

  open fun updateStateForTombstone(messageKey: EventMessageKey): Unit =
      throw NotImplementedError("Unhandled tombstone message: $messageKey")

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractStateStrategy::class.java)
  }
}
