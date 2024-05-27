/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamContext
import org.apache.avro.specific.SpecificRecordBase

class FeaturetoggleServiceEventStreamContext(
    events: MutableMap<String, SpecificRecordBase>,
    lastIdentifierPerType: MutableMap<String, AggregateIdentifierAvro>,
    timeLineGenerator: TimeLineGenerator,
    private val onlineListener: MutableMap<String, List<KafkaListenerFunction>> = mutableMapOf(),
    private val restoreListener: MutableMap<String, List<KafkaListenerFunction>> = mutableMapOf(),
) : EventStreamContext(events, lastIdentifierPerType, timeLineGenerator, mutableMapOf()) {

  override fun send(runnable: Runnable) {
    simulateKafkaListener { runnable.run() }
  }

  fun useOnlineListener(): FeaturetoggleServiceEventStreamContext {
    this.listeners.apply {
      clear()
      putAll(onlineListener)
    }
    return this
  }

  fun useRestoreListener(): FeaturetoggleServiceEventStreamContext {
    this.listeners.apply {
      clear()
      putAll(restoreListener)
    }
    return this
  }
}
