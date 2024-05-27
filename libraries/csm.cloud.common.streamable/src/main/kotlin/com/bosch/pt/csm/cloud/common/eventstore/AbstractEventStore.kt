/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_WRITING_OPERATIONS
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.tracing.TraceUtils.traceWithNewSpan
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.opentracing.Span
import jakarta.persistence.EntityManager
import java.lang.String.join
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Optional
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.utils.Utils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

/**
 * Abstract implementation of an event store. The store receives an avro class (representing an
 * event) together with a message key, serializes both, determined the topic partitions and stores
 * the result as an instance of a sub-class of [AbstractKafkaEvent] in a local database table. These
 * events are transferred to Kafka using our kafka connector, which is an external component reading
 * from the event table and sending the already serialized messages to kafka. This implements the
 * outbox pattern.
 *
 * Why are events serialized already here? We need to serialize the avro classes in some way in
 * order to store them in a common database table that contains a column for the message key and
 * another one for the message itself. So we chose the serialization that is in anyway required when
 * sending to kafka so that this does not have to be done again. Another reason is, that the
 * external kafka connector that transfers the events to a kafka topic can be verify simple and
 * reusable for all context since it does not need any knowledge about the event schemas.
 *
 * Why is the partition determination done here? We want to have all events for the same project in
 * the same topic partition. Since we use a complex avro class with multiple attributes as the
 * message key, we cannot rely on the default partition determination strategy. We are only using
 * the attribute "rootContextIdentifier" of the message key for the partition determination. This
 * contains the identifier of a project, or a user or a company, for example.
 *
 * In addition to the actual kafka message, we also store tracing information that is forwarded as a
 * kafka header to all downstream services. This way, our traces also cover all services just
 * connected via kafka topics.
 *
 * In case a tombstone message is stored in the event store, the save method generates tombstone
 * messages for all previous versions of targeted aggregate instance. This way, kafkas log
 * compaction feature removes all messages eventually from the topic for the targeted aggregate
 * instance.
 */
abstract class AbstractEventStore<E : AbstractKafkaEvent>(
    @Value("\${block-modifying-operations:false}") val blockModifyingOperations: Boolean = false,
    private val em: EntityManager,
    kafkaProperties: KafkaProperties,
    environment: Environment,
    schemaRegistryClient: Optional<SchemaRegistryClient>
) {
  // Don't use KafkaAvroSerializer as an injected bean because it cannot be mocked
  private var kafkaAvroSerializer: Serializer<Any> =
      when (environment.acceptsProfiles(Profiles.of("test"))) {
        true -> KafkaAvroSerializer(schemaRegistryClient.get(), kafkaProperties.properties)
        else -> KafkaAvroSerializer(schemaRegistryClient.orElse(null), kafkaProperties.properties)
      }

  @Transactional(propagation = MANDATORY)
  open fun save(key: AggregateEventMessageKey, value: SpecificRecord? = null) {
    assertModifyingOperationsNotBlocked()
    val topic = getNameOfEventTopic()

    traceWithNewSpan(operationName = "send events to kafka") { tracer, span ->
      if (value == null) {
        for (tombstoneKey in key.toListOfTombstoneMessageKeys()) {
          createKafkaEvent(
                  "b3",
                  toB3SingleHeader(span, tracer.activeSpan()),
                  partitionOf(tombstoneKey),
                  kafkaAvroSerializer.serialize(topic, tombstoneKey.toAvro()),
                  null)
              .also { em.persist(it) }
        }
      } else {
        createKafkaEvent(
                "b3",
                toB3SingleHeader(span, tracer.activeSpan()),
                partitionOf(key),
                kafkaAvroSerializer.serialize(topic, key.toAvro()),
                kafkaAvroSerializer.serialize(topic, value))
            .also { em.persist(it) }
      }
    }
  }

  /**
   * Generates tombstone message keys for all previous versions plus the current version of the
   * aggregate referenced by the given message key
   */
  private fun AggregateEventMessageKey.toListOfTombstoneMessageKeys() =
      ArrayList<AggregateEventMessageKey>().also {
        for (version in 0L..this.aggregateIdentifier.version) {
          it.add(
              AggregateEventMessageKey(
                  this.aggregateIdentifier.copy(version = version), this.rootContextIdentifier))
        }
      }

  abstract fun getNameOfEventTopic(): String

  abstract fun getNumberOfPartitionsOfEventTopic(): Int

  abstract fun createKafkaEvent(
      traceHeaderKey: String,
      traceHeaderValue: String,
      partitionNumber: Int,
      eventKey: ByteArray,
      event: ByteArray? = null
  ): E

  private fun partitionOf(keyAvro: EventMessageKey): Int =
      Utils.toPositive(Utils.murmur2(keyAvro.rootContextIdentifier.toString().toByteArray(UTF_8))) %
          getNumberOfPartitionsOfEventTopic()

  private fun toB3SingleHeader(currentSpan: Span, parentSpan: Span): String {
    val traceId = currentSpan.context().toTraceId()
    val spanId = currentSpan.context().toSpanId()
    val parentSpanId = parentSpan.context().toSpanId()

    // construct B3 single header format: {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
    return join("-", traceId, spanId, B3_SAMPLING_STATE_ACCEPT, parentSpanId)
  }

  private fun assertModifyingOperationsNotBlocked() {
    if (blockModifyingOperations) {
      throw BlockOperationsException(BLOCK_WRITING_OPERATIONS)
    }
  }

  companion object {
    const val B3_SAMPLING_STATE_ACCEPT = "1"
  }
}
