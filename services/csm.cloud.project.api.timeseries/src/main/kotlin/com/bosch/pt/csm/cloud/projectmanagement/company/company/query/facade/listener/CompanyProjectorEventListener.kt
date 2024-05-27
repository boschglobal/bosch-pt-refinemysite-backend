/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.service.CompanyProjector
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-company-projector-listener-disabled")
@Component
class CompanyProjectorEventListener(
    private val logger: Logger,
    private val companyProjector: CompanyProjector
) : CompanyEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.company.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.company-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.company-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.company-projector.concurrency}")
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == COMPANY.name) {

      when (event) {
        null -> error("Unable to handle company tombstone message.")
        is CompanyEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> companyProjector.onCompanyEvent(event.aggregate)
              DELETED -> companyProjector.onCompanyDeletedEvent(event.aggregate)
              else -> error("Unhandled project event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
