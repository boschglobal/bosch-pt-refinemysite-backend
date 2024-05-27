/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.ConsumerBusinessTransactionManager
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.AbstractBusinessTransactionAwareListener
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Profile("!kafka-project-listener-disabled")
@Component
class ProjectKafkaEventListener(
    businessTransactionManager: ConsumerBusinessTransactionManager,
    eventProcessor: ProjectEventProcessor,
    private val transactionTemplate: TransactionTemplate
) :
    AbstractBusinessTransactionAwareListener(businessTransactionManager, eventProcessor),
    ProjectEventListener {

  @KafkaListener(topics = ["#{kafkaTopicConfiguration.getTopicForChannel('project')}"])
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    transactionTemplate.executeWithoutResult { super.process(record) }

    ack.acknowledge()
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ProjectKafkaEventListener::class.java)
  }
}
