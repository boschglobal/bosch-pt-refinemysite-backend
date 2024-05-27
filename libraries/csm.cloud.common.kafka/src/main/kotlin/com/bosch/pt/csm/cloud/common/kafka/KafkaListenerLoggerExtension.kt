/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.kafka

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.messages.CommandMessageKeyAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger

@ExcludeFromCodeCoverage
fun Logger.logConsumption(record: ConsumerRecord<*, *>) {

  if (!this.isDebugEnabled) return

  val keyValue =
      record.key()?.let {
        when (it) {
          is AggregateEventMessageKey ->
              "${it.aggregateIdentifier.type}/" +
                  "${it.aggregateIdentifier.identifier}/" +
                  "${it.aggregateIdentifier.version}"
          is CommandMessageKey -> it.partitioningIdentifier.toString()
          else -> it.javaClass.simpleName
        }
      }

  this.debug(
      "Received message of type {} with key {} from topic {}, partition {}, offset {}",
      record.value()?.javaClass?.simpleName ?: "<Tombstone>",
      keyValue ?: "<empty>",
      record.topic(),
      record.partition(),
      record.offset())

  if (this.isTraceEnabled) this.trace(record.value().toString())
}

@ExcludeFromCodeCoverage
fun Logger.logProduction(record: ProducerRecord<*, *>) {

  if (!this.isDebugEnabled) return

  val keyValue =
      record.key()?.let {
        when (it) {
          is MessageKeyAvro ->
              "${it.getAggregateIdentifier().getType()}/" +
                  "${it.getAggregateIdentifier().getIdentifier()}/" +
                  "${it.getAggregateIdentifier().getVersion()}"
          is CommandMessageKeyAvro -> it.getPartitioningIdentifier().toString()
          else -> it.javaClass.simpleName
        }
      }

  this.debug(
      "Published message of type {} with key {} to topic {}, partition {}",
      record.value()?.javaClass?.simpleName ?: "<Tombstone>",
      keyValue ?: "<empty>",
      record.topic(),
      record.partition())

  if (this.isTraceEnabled) this.trace(record.value().toString())
}
