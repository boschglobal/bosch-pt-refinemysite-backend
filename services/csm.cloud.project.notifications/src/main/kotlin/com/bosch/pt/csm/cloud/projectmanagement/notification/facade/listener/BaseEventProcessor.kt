/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.application.config.ProcessStateOnlyProperties
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.StateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import java.time.LocalDate
import org.slf4j.LoggerFactory

open class BaseEventProcessor(
    private val updateStateStrategies: Set<UpdateStateStrategy>,
    private val cleanUpStateStrategies: Set<CleanUpStateStrategy>,
    private val processStateOnlyProperties: ProcessStateOnlyProperties
) {

  protected fun shouldProcessStateOnly(record: EventRecord): Boolean {
    if (!processStateOnlyProperties.enabled) {
      return false
    }
    val stateOnlyUntilDate = LocalDate.parse(processStateOnlyProperties.untilDate)
    val eventDate = record.messageDate.toLocalDate()
    return eventDate.isBefore(stateOnlyUntilDate)
  }

  protected fun updateState(record: EventRecord) =
      modifyState(record, updateStateStrategies, "Update")

  protected fun cleanUpState(record: EventRecord) =
      modifyState(record, cleanUpStateStrategies, "Clean-Up")

  private fun modifyState(
      record: EventRecord,
      strategies: Set<StateStrategy>,
      strategyType: String
  ) {
    val numberOfStrategiesApplied =
        strategies
            .filter { it.handles(record) }
            .map {
              it.apply(record)
              1
            }
            .count()
    if (numberOfStrategiesApplied == 0) {
      val type =
          record.value?.javaClass
              ?: record.key.let {
                when (it) {
                  is AggregateEventMessageKey -> "${it.aggregateIdentifier.type} tombstone"
                  else -> "${it.javaClass} tombstone"
                }
              }

      LOGGER.debug("No {} State Strategy applied for event type {}", strategyType, type)
    }
  }

  override fun toString() =
      // workaround to avoid illegal reflection access warning
      // by spring proxies (due to java 11)
      "${javaClass.name}@${Integer.toHexString(hashCode())}"

  companion object {
    private val LOGGER = LoggerFactory.getLogger(BaseEventProcessor::class.java)
  }
}
