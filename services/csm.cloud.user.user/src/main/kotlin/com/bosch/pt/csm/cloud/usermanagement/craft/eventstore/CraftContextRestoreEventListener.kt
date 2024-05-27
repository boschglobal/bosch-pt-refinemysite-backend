/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.RestoreFromKafkaAdapter
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.referencedata.craft.event.listener.CraftEventListener
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Profile("restore-db & !kafka-craft-listener-disabled")
@Component
class CraftContextRestoreEventListener(
    transactionTemplate: TransactionTemplate,
    eventBus: CraftContextLocalEventBus,
    private val logger: Logger
) : CraftEventListener, RestoreFromKafkaAdapter(transactionTemplate, eventBus) {

  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('craft')}"],
      clientIdPrefix = "csm-cloud-user-craft-restore")
  override fun listenToCraftEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)
    emit(record)
    ack.acknowledge()
  }
}
