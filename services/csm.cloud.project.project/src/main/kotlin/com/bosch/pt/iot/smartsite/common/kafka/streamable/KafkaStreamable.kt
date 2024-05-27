/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.kafka.streamable

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

/** Interface to be implemented by any object whose changes are sent as streams. */
interface KafkaStreamable {

  /**
   * Transforms an already serialized avro message (byte array) to an event entry.
   *
   * @param key the serialized avro message key
   * @param payload the serialized avro message
   * @return an instance of [AbstractKafkaEvent]
   */
  fun toEvent(
      key: ByteArray,
      payload: ByteArray?,
      partition: Int,
      transactionId: UUID?
  ): AbstractKafkaEvent

  /**
   * Transforms the object that implements this interface to an Avro
   *
   * @return an instance of a generated AVRO class (a sub-class of [SpecificRecord]) representing
   * the event
   */
  fun toAvroMessage(): SpecificRecord?

  /** Determines the message key of this streamable. */
  fun toMessageKey(): EventMessageKey

  /**
   * Creates a list of message keys to be sent as tombstone messages to delete all previous versions
   * of the aggregate in kafka.
   *
   * @return returns a list of message keys for all entity versions from 0..N. Should return an
   * empty list if eventName is not "DELETE".
   */
  fun toAvroTombstoneMessageKeys(): List<AggregateEventMessageKey> = emptyList()

  /**
   * Used to specify a channel name for a kafkaStreamable object that is used as the key of the
   * [KafkaTopicProperties.bindings] map.
   *
   * @return the channel name
   */
  fun getChannel(): String
}
