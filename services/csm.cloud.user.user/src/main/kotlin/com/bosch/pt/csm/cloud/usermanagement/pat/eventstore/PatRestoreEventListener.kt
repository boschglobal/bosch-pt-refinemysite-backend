/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.pat.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.RestoreFromKafkaAdapter
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.pat.event.listener.PatEventListener
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Profile("restore-db & !kafka-pat-listener-disabled")
@Component
class PatRestoreEventListener(
    transactionTemplate: TransactionTemplate,
    eventBus: PatLocalEventBus,
    private val logger: Logger
) : PatEventListener, RestoreFromKafkaAdapter(transactionTemplate, eventBus) {

  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('pat')}"],
      clientIdPrefix = "csm-cloud-user-pat-restore")
  override fun listenToPatEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)
    emit(record)
    ack.acknowledge()
  }
}
