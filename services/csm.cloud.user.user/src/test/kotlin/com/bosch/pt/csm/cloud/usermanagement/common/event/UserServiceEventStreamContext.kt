/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamContext
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.simulateKafkaListener
import org.apache.avro.specific.SpecificRecordBase

class UserServiceEventStreamContext(
    events: MutableMap<String, SpecificRecordBase>,
    lastIdentifierPerType: MutableMap<String, AggregateIdentifierAvro>,
    timeLineGenerator: TimeLineGenerator,
    private val onlineListener: MutableMap<String, List<KafkaListenerFunction>> = mutableMapOf(),
    private val restoreListener: MutableMap<String, List<KafkaListenerFunction>> = mutableMapOf(),
) : EventStreamContext(events, lastIdentifierPerType, timeLineGenerator, mutableMapOf()) {

  override fun send(runnable: Runnable) {
    simulateKafkaListener { runnable.run() }
  }

  fun useOnlineListener(): UserServiceEventStreamContext {
    this.listeners.apply {
      clear()
      putAll(onlineListener)
    }
    return this
  }

  fun useRestoreListener(): UserServiceEventStreamContext {
    this.listeners.apply {
      clear()
      putAll(restoreListener)
    }
    return this
  }
}
