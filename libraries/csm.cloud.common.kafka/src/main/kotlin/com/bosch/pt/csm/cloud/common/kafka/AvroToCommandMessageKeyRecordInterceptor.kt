/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafka

import com.bosch.pt.csm.cloud.common.messages.CommandMessageKeyAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyFactory.createCommandMessageKey
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RecordInterceptor

/**
 * Converts the record key type from Avro (e.g. [CommandMessageKeyAvro]) to its corresponding
 * subclass of [CommandMessageKey].
 *
 * When using this interceptor, Kafka listeners must express the consumer record as follows:
 * ```
 *    ConsumerRecord<CommandMessageKey, SpecificRecordBase>
 * ```
 */
class AvroToCommandMessageKeyRecordInterceptor : RecordInterceptor<Any, Any> {

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
          createCommandMessageKey(key() as GenericRecord),
          value(),
          headers(),
          leaderEpoch())
    }
  }
}
