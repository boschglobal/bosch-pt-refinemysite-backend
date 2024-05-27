/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafka

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyFactory.createEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RecordInterceptor

/**
 * Converts the record key type from Avro (e.g. [MessageKeyAvro]) to its corresponding subclass of
 * [EventMessageKey].
 *
 * When using this interceptor, Kafka listeners must express the consumer record as follows:
 * ```
 *    ConsumerRecord<EventMessageKey, SpecificRecordBase?>
 * ```
 */
class AvroToEventMessageKeyRecordInterceptor : RecordInterceptor<Any, Any> {

  override fun intercept(
      record: ConsumerRecord<Any, Any>,
      consumer: Consumer<Any, Any>
  ): ConsumerRecord<Any, Any> {
    return with(record) {
      ConsumerRecord(
          topic(),
          partition(),
          offset(),
          timestamp(),
          timestampType(),
          serializedKeySize(),
          serializedValueSize(),
          createEventMessageKey(key() as GenericRecord),
          value(),
          headers(),
          leaderEpoch())
    }
  }
}
