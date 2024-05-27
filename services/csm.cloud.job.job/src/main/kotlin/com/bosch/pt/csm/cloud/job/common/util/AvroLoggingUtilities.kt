/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.job.common.util

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

@LibraryCandidate
@ExcludeFromCodeCoverage
object AvroLoggingUtilities {

  private val LOGGER = LoggerFactory.getLogger(AvroLoggingUtilities::class.java)

  fun logEventPublishing(record: ProducerRecord<*, *>) {
    LOGGER.debug(
        "Published event of type {} with key {} to topic {}, partition {}",
        getTypeName(record.value()),
        getKeyValue(record.key()),
        record.topic(),
        record.partition())
  }

  fun logCommandConsumption(record: ConsumerRecord<*, *>) {
    LOGGER.debug(
        "Received command of type {} with key {} from topic {}, partition {}, offset {}",
        getTypeName(record.value()),
        getKeyValue(record.key()),
        record.topic(),
        record.partition(),
        record.offset())
  }

  fun logEventConsumption(record: ConsumerRecord<*, *>) {
    LOGGER.debug(
        "Received event of type {} with key {} from topic {}, partition {}, offset {}",
        getTypeName(record.value()),
        getKeyValue(record.key()),
        record.topic(),
        record.partition(),
        record.offset())
  }

  private fun getTypeName(value: Any?) = value?.javaClass?.simpleName ?: "<Tombstone>"

  private fun getKeyValue(key: Any) =
      if (key is MessageKeyAvro) {
        "${key.getAggregateIdentifier().getType()}/" +
            "${key.getAggregateIdentifier().getIdentifier()}/" +
            "${key.getAggregateIdentifier().getVersion()}"
      } else {
        key.javaClass.simpleName
      }
}
