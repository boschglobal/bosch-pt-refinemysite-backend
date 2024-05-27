/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.restoredb

import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

abstract class AbstractRestoreDbEventListener(
    private val offsetSynchronizationManager: OffsetSynchronizationManager
) {

  fun shouldWaitUntilProcessedInOnlineService(
      record: ConsumerRecord<*, SpecificRecordBase?>
  ): Boolean {
    val maxPartitionOffset =
        offsetSynchronizationManager.getMaxTopicPartitionOffset(record.topic(), record.partition())

    if (maxPartitionOffset == null || record.offset() > maxPartitionOffset) {
      LOGGER.info(
          "Offset of message to restore is higher than the already committed offset of the service." +
              " Topic: " +
              record.topic() +
              " Partition: " +
              record.partition() +
              " Offset: " +
              record.offset() +
              " Postpone processing...")

      return true
    }
    return false
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractRestoreDbEventListener::class.java)
  }
}
