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
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-company-listener-disabled")
@Component
class CompanyEventListenerImpl(
    private val companyEventToStateProcessor: CompanyEventToStateProcessor,
) : CompanyEventListener {

  @Trace
  @KafkaListener(topics = ["#{kafkaTopicConfiguration.getTopicForChannel('company')}"])
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    val message = record.value()

    companyEventToStateProcessor.updateStateFromCompanyEvent(message!!)
    ack.acknowledge()
  }

  /*
   * workaround to avoid illegal reflection access warning
   * by spring proxies (due to java 11)
   */
  @ExcludeFromCodeCoverage
  override fun toString() = this.javaClass.name + "@" + Integer.toHexString(this.hashCode())

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CompanyEventListenerImpl::class.java)
  }
}
