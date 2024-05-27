/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.RestoreFromKafkaAdapter
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Profile("restore-db & !company-context-restore-snapshots-event-listener-disabled")
@Component
class CompanyContextRestoreSnapshotsEventListener(
    transactionTemplate: TransactionTemplate,
    eventBus: CompanyContextLocalEventBus,
    private val logger: Logger
) : CompanyEventListener, RestoreFromKafkaAdapter(transactionTemplate, eventBus) {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('company')}"],
      clientIdPrefix = "csm-cloud-company-company-restore")
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)
    emit(record)
    ack.acknowledge()
  }
}
