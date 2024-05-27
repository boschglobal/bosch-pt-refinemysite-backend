/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.facade.listener.restore

import com.bosch.pt.csm.cloud.common.exception.RestoreServiceAheadOfOnlineServiceException
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.AbstractRestoreDbEventListener
import com.bosch.pt.csm.cloud.common.streamable.restoredb.OffsetSynchronizationManager
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.referencedata.craft.event.listener.CraftEventListener
import com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy.CraftContextRestoreDbStrategy
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional

@Profile("restore-db & !kafka-craft-listener-disabled")
@Component
open class RestoreDbCraftEventListenerImpl(
    private val craftRestoreDbStrategyDispatcher:
        RestoreDbStrategyDispatcher<CraftContextRestoreDbStrategy>,
    offsetSynchronizationManager: OffsetSynchronizationManager
) : AbstractRestoreDbEventListener(offsetSynchronizationManager), CraftEventListener {

  @Transactional(propagation = NEVER)
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('craft')}"],
      clientIdPrefix = "csm-cloud-project-craft-restore")
  override fun listenToCraftEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {

    if (shouldWaitUntilProcessedInOnlineService(record)) {
      throw RestoreServiceAheadOfOnlineServiceException(record)
    }

    LOGGER.logConsumption(record)
    craftRestoreDbStrategyDispatcher.dispatch(record)

    ack.acknowledge()
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(RestoreDbCraftEventListenerImpl::class.java)
  }
}
