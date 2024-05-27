/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener.LiveUpdateEventProcessor
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Profile("!kafka-user-listener-disabled")
@Component
class UserKafkaEventListener(
    private val liveUpdateEventProcessor: LiveUpdateEventProcessor,
    private val userEventToStateProcessor: UserEventToStateProcessor,
    private val transactionTemplate: TransactionTemplate
) : UserEventListener {

  @KafkaListener(topics = ["#{kafkaTopicConfiguration.getTopicForChannel('user')}"])
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {

    LOGGER.logConsumption(record)
    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    transactionTemplate.executeWithoutResult {
      val message = record.value()

      if (message == null) {
        handleUserContextTombstoneMessage(record.key() as AggregateEventMessageKey)
      } else {
        userEventToStateProcessor.updateStateFromUserEvent(message)
      }
      liveUpdateEventProcessor.processUserEvents(
          record.key(), record.value(), record.timestamp().toLocalDateTimeByMillis())
    }

    ack.acknowledge()
  }

  private fun handleUserContextTombstoneMessage(key: AggregateEventMessageKey) {
    if (key.aggregateIdentifier.type == USER.value) {
      userEventToStateProcessor.deleteUser(key.aggregateIdentifier.identifier)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserKafkaEventListener::class.java)
  }
}
