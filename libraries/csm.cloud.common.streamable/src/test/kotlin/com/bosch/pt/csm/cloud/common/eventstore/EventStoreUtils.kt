/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.exception.UnexpectedEventsException
import com.bosch.pt.csm.cloud.common.eventstore.exception.WrongEventSequenceException
import com.bosch.pt.csm.cloud.common.eventstore.exception.WrongNumberOfEventsException
import com.bosch.pt.csm.cloud.common.eventstore.exception.WrongNumberOfTombstonesException
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.GenericEnumSymbol
import org.apache.avro.specific.SpecificRecordBase

/**
 * This version of EventStoreUtils was extended compared to those in the company and project
 * service. It also provides methods to verify tombstone messages.
 */
class EventStoreUtils<T : AbstractKafkaEvent>(
    private val kafkaEventRepository: KafkaEventTestRepository<T>,
    mockSchemaRegistryClient: SchemaRegistryClient
) {

  private val kafkaAvroDeserializer: KafkaAvroDeserializer =
      KafkaAvroDeserializer(
          mockSchemaRegistryClient,
          mapOf("schema.registry.url" to "mock://", "specific.avro.reader" to "true"))

  /** Deletes all events stored so far in the project events table */
  fun reset() {
    kafkaEventRepository.deleteAll()
  }

  /** Verifies that no event has been written so far. */
  fun verifyEmpty() {
    val deserializedEvents = getDeserializedEvents()
    if (deserializedEvents.isNotEmpty()) {
      throw UnexpectedEventsException(deserializedEvents, deserializedEvents.size)
    }
  }

  /** Verifies the number of events in the event table. */
  fun verifyNumberOfEvents(expectedEvents: Int) {
    val deserializedEvents = getDeserializedEvents()
    if (deserializedEvents.size != expectedEvents) {
      throw WrongNumberOfEventsException(expectedEvents, deserializedEvents.size)
    }
  }

  /**
   * Verifies that a certain number of events have been created.
   *
   * @param eventType avro class of the expected event type
   * @param eventName enum value of the expected event name
   * @param occurrences expected number of events of given type and name
   * @param verifyNoOtherEventsExist if true, other event types found will throw an exception
   */
  @JvmOverloads
  fun <E : SpecificRecordBase> verifyContains(
      eventType: Class<E>,
      eventName: GenericEnumSymbol<*>?,
      occurrences: Int,
      verifyNoOtherEventsExist: Boolean = true
  ) = verifyContainsAndGet(eventType, eventName, occurrences, verifyNoOtherEventsExist)

  /**
   * Verifies that one event has been created. Returns the found event.
   *
   * @param eventType avro class of the expected event type
   * @param eventName enum value of the expected event name; null, if the name should not be checked
   * @param verifyNoOtherEventsExist if true, other event types found will throw an exception
   * @return The found event
   */
  @JvmOverloads
  fun <E : SpecificRecordBase> verifyContainsAndGet(
      eventType: Class<E>,
      eventName: GenericEnumSymbol<*>?,
      verifyNoOtherEventsExist: Boolean = true,
  ): E = verifyContainsAndGet(eventType, eventName, 1, verifyNoOtherEventsExist)[0]

  /**
   * Verifies that a certain number of events have been created. Returns the found the events.
   *
   * @param eventType avro class of the expected event type
   * @param eventName enum value of the expected event name; null, if the name should not be checked
   * @param occurrences expected number of events of given type and name
   * @param verifyNoOtherEventsExist if true, other event types found will throw an exception
   * @return The list of found events
   */
  @Suppress("ThrowsCount")
  fun <E : SpecificRecordBase> verifyContainsAndGet(
      eventType: Class<E>,
      eventName: GenericEnumSymbol<*>?,
      occurrences: Int,
      verifyNoOtherEventsExist: Boolean = true,
  ): List<E> {
    val deserializedEvents = getDeserializedEvents()
    val filteredEvents =
        deserializedEvents
            .filter { eventType.isInstance(it) }
            .filter { eventName == null || hasEventName(it, eventName) }
            .map(eventType::cast)
            .toList()
    if (filteredEvents.size != occurrences) {
      if (eventName == null) {
        throw WrongNumberOfEventsException(eventType.name, occurrences, filteredEvents.size)
      } else {
        throw WrongNumberOfEventsException(
            eventType.name, eventName.toString(), occurrences, filteredEvents.size)
      }
    }
    if (verifyNoOtherEventsExist && deserializedEvents.size != filteredEvents.size) {
      val unexpectedEvents = deserializedEvents.filter { !filteredEvents.contains(it) }.toList()
      throw UnexpectedEventsException(unexpectedEvents, unexpectedEvents.size)
    }
    return filteredEvents
  }

  fun verifyContainsInSequence(vararg eventTypes: Class<out SpecificRecordBase>) =
      verifyContainsInSequence(listOf(*eventTypes))

  fun verifyContainsInSequence(eventTypes: List<Class<out SpecificRecordBase>>) {
    val deserializedEvents = getDeserializedEvents()
    if (deserializedEvents.size != eventTypes.size) {
      throw WrongNumberOfEventsException(eventTypes.size, deserializedEvents.size)
    }
    deserializedEvents.toTypedArray().forEachIndexed { index, specificRecordBase ->
      if (!eventTypes[index].isInstance(specificRecordBase)) {
        throw WrongEventSequenceException(eventTypes, deserializedEvents)
      }
    }
  }

  /**
   * Verifies that a single tombstone message has been created.
   *
   * @param occurrences the number of expected tombstone messages
   * @param verifyNoOtherEventsExist if true, other event types found will throw an exception
   */
  @JvmOverloads
  fun verifyContainsTombstoneMessages(
      occurrences: Int,
      aggregateType: String? = null,
      verifyNoOtherEventsExist: Boolean = true,
  ) = verifyContainsTombstoneMessageAndGet(occurrences, aggregateType, verifyNoOtherEventsExist)

  /**
   * Verifies that a single tombstone message has been created. Returns the found tombstone message.
   *
   * @param verifyNoOtherEventsExist if true, other event types found will throw an exception
   * @return The key of the found tombstone message
   */
  @JvmOverloads
  fun verifyContainsTombstoneMessageAndGet(
      aggregateType: String? = null,
      verifyNoOtherEventsExist: Boolean = true,
  ): MessageKeyAvro =
      verifyContainsTombstoneMessageAndGet(1, aggregateType, verifyNoOtherEventsExist)[0]

  /**
   * Verifies that a certain number of tombstone messages have been created. Returns the found list
   * of tombstone messages.
   *
   * @param occurrences expected number of tombstone messages
   * @param verifyNoOtherEventsExist if true, other event types found will throw an exception
   * @return A list of keys of the found tombstone messages
   */
  @JvmOverloads
  fun verifyContainsTombstoneMessageAndGet(
      occurrences: Int,
      aggregateType: String? = null,
      verifyNoOtherEventsExist: Boolean = true,
  ): List<MessageKeyAvro> {

    getNumberOfTombstoneEventsOfType(aggregateType).also {
      if (it != occurrences) {
        throw WrongNumberOfTombstonesException(aggregateType ?: "<various>", occurrences, it)
      }
    }
    val nonTombstoneEvents =
        kafkaEventRepository.findAll().map { it.event }.filterNotNull().toList()
    if (verifyNoOtherEventsExist && nonTombstoneEvents.isNotEmpty()) {
      throw UnexpectedEventsException(
          nonTombstoneEvents.map(::deserialize).toList(), nonTombstoneEvents.size)
    }

    return getDeserializedEventKeys()
  }

  private fun getNumberOfTombstoneEventsOfType(aggregateType: String?) =
      kafkaEventRepository
          .findAll()
          .asSequence()
          .filter { it.event == null }
          .map { it.eventKey }
          .map(::deserialize)
          .count { isAggregateType(it, aggregateType) }

  private fun getDeserializedEvents() =
      kafkaEventRepository.findAll().mapNotNull { it.event }.map(::deserialize).toList()

  private fun getDeserializedEventKeys() =
      kafkaEventRepository
          .findAll()
          .map { it.eventKey }
          .map(::deserialize)
          .map { key -> key as MessageKeyAvro }
          .toList()

  private fun deserialize(serializedEvent: ByteArray) =
      kafkaAvroDeserializer.deserialize(null, serializedEvent) as SpecificRecordBase

  private fun hasEventName(record: SpecificRecordBase, eventName: GenericEnumSymbol<*>?) =
      eventName == null || record["name"].toString() == eventName.toString()

  private fun isAggregateType(record: SpecificRecordBase, aggregateType: String?) =
      aggregateType.isNullOrBlank() ||
          (record["aggregateIdentifier"] as SpecificRecordBase)["type"].toString() == aggregateType
}
