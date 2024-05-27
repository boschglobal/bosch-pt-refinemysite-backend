/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.projectmanagement.application.config.ProcessStateOnlyProperties
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.BaseEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
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
    updateStateStrategies: Set<UpdateStateStrategy>,
    cleanUpStateStrategies: Set<CleanUpStateStrategy>,
    processStateOnlyProperties: ProcessStateOnlyProperties,
) :
    BaseEventProcessor(updateStateStrategies, cleanUpStateStrategies, processStateOnlyProperties),
    CompanyEventListener {

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.bindings.company.kafkaTopic}"])
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    val eventRecord = record.toEventRecord()
    updateState(eventRecord)
    cleanUpState(eventRecord)
    ack.acknowledge()
  }

  override fun toString() =
      // workaround to avoid illegal reflection access warning
      // by spring proxies (due to java 11)
      "${javaClass.name}@${Integer.toHexString(hashCode())}"

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CompanyEventListenerImpl::class.java)
  }

  private fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.toEventRecord() =
      EventRecord(this.key(), this.value(), this.timestamp().toLocalDateTimeByMillis())
}
