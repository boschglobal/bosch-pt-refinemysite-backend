/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
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
class UserEventListenerImpl(private val userEventToStateProcessor: UserEventToStateProcessor) :
    UserEventListener {

  @Trace
  @KafkaListener(topics = ["#{kafkaTopicConfiguration.getTopicForChannel('user')}"])
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    val key = record.key()

    when (val value = record.value()) {
      null -> handleUserContextTombstoneMessage(key)
      else -> userEventToStateProcessor.updateStateFromUserEvent(value)
    }

    ack.acknowledge()
  }

  /*
   * workaround to avoid illegal reflection access warning
   * by spring proxies (due to java 11)
   */
  @ExcludeFromCodeCoverage
  override fun toString() = this.javaClass.name + "@" + Integer.toHexString(this.hashCode())

  private fun handleUserContextTombstoneMessage(key: EventMessageKey) {
    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == USER.value) {
      userEventToStateProcessor.deleteUser(key.aggregateIdentifier.identifier)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserEventListenerImpl::class.java)
  }
}
