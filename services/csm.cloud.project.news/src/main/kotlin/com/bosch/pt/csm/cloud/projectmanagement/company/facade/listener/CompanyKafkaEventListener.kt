/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Profile("!kafka-company-listener-disabled")
@Component
class CompanyKafkaEventListener(
    private val companyEventToStateProcessor: CompanyEventToStateProcessor,
    private val transactionTemplate: TransactionTemplate
) : CompanyEventListener {

  @KafkaListener(topics = ["#{kafkaTopicConfiguration.getTopicForChannel('company')}"])
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    check(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    transactionTemplate.executeWithoutResult {
      companyEventToStateProcessor.updateStateFromCompanyEvent(record.value())
    }

    ack.acknowledge()
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CompanyKafkaEventListener::class.java)
  }
}
