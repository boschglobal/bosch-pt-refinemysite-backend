/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.facade.listener.restore

import com.bosch.pt.csm.cloud.common.exception.RestoreServiceAheadOfOnlineServiceException
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.AbstractRestoreDbEventListener
import com.bosch.pt.csm.cloud.common.streamable.restoredb.OffsetSynchronizationManager
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy.CompanyContextRestoreDbStrategy
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional

@Profile("restore-db & !kafka-company-listener-disabled")
@Component
open class RestoreDbCompanyEventListenerImpl(
    private val companyRestoreDbStrategyDispatcher:
        RestoreDbStrategyDispatcher<CompanyContextRestoreDbStrategy>,
    offsetSynchronizationManager: OffsetSynchronizationManager
) : AbstractRestoreDbEventListener(offsetSynchronizationManager), CompanyEventListener {

  @Trace
  @Transactional(propagation = NEVER)
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('company')}"],
      clientIdPrefix = "csm-cloud-project-company-restore")
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {

    if (shouldWaitUntilProcessedInOnlineService(record)) {
      throw RestoreServiceAheadOfOnlineServiceException(record)
    }

    LOGGER.logConsumption(record)
    companyRestoreDbStrategyDispatcher.dispatch(record)

    ack.acknowledge()
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(RestoreDbCompanyEventListenerImpl::class.java)
  }
}
