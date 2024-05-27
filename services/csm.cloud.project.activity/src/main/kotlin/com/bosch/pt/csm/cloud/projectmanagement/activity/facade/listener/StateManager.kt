/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.listener

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.StateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StateManager(
    private val updateStateStrategies: Set<UpdateStateStrategy>,
    private val cleanUpStateStrategies: Set<CleanUpStateStrategy>
) {

  fun updateState(key: EventMessageKey, value: SpecificRecordBase?) {
    val numberOfStrategiesApplied = modifyState(key, value, updateStateStrategies)
    if (numberOfStrategiesApplied == 0) {
      LOGGER.debug("No update state strategy applied for event with key $key")
    }
  }

  fun cleanUpState(key: EventMessageKey, value: SpecificRecordBase?) {
    val numberOfStrategiesApplied = modifyState(key, value, cleanUpStateStrategies)
    if (numberOfStrategiesApplied == 0) {
      LOGGER.debug("No clean up strategy applied for event with key $key")
    }
  }

  private fun modifyState(
      key: EventMessageKey,
      value: SpecificRecordBase?,
      strategies: Set<StateStrategy>,
  ): Int = strategies.filter { it.handles(key, value) }.map { it.apply(key, value) }.count()

  companion object {
    private val LOGGER = LoggerFactory.getLogger(StateManager::class.java)
  }
}
