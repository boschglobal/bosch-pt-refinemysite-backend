/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.relation.facade.listener.online

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.ConsumerBusinessTransactionManager
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.AbstractBusinessTransactionAwareListener
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive
import org.springframework.transaction.support.TransactionTemplate

@Profile("!restore-db & !kafka-project-listener-disabled")
@Component
open class CalculateRelationCriticalityListener(
    businessTransactionManager: ConsumerBusinessTransactionManager,
    eventProcessor: CalculateRelationCriticalityEventProcessor,
    val transactionTemplate: TransactionTemplate
) :
    ProjectEventListener,
    AbstractBusinessTransactionAwareListener(businessTransactionManager, eventProcessor) {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.relation-criticality.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.relation-criticality.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.relation-criticality.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    require(!isActualTransactionActive()) { "No running transaction expected" }
    transactionTemplate.executeWithoutResult { super.process(record) }
    ack.acknowledge()
  }
}
