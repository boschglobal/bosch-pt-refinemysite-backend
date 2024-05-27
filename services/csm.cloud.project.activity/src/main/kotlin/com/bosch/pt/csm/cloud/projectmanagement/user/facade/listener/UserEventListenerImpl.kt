/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.listener.StateManager
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-user-listener-disabled")
@Component
class UserEventListenerImpl(private val stateManager: StateManager) : UserEventListener {

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.bindings.user.kafkaTopic}"])
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    val key = record.key()
    val value = record.value()

    stateManager.updateState(key, value)
    stateManager.cleanUpState(key, value)
    ack.acknowledge()
  }

  /** Workaround to avoid illegal reflection access warning by spring proxies (due to java 11) */
  override fun toString() = "${javaClass.name}@${Integer.toHexString(hashCode())}"

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserEventListener::class.java)
  }
}
