/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.kafka.eventstore

import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_WRITING_OPERATIONS
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder.currentBusinessTransactionId
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamable
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.opentracing.Span
import io.opentracing.log.Fields.ERROR_OBJECT
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import jakarta.persistence.EntityManager
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Optional
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.utils.Utils.murmur2
import org.apache.kafka.common.utils.Utils.toPositive
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Component
open class EventStoreImpl(
    private val em: EntityManager,
    kafkaProperties: KafkaProperties,
    private val kafkaTopicProperties: KafkaTopicProperties,
    environment: Environment,
    // The schema registry dependency is used for test scenarios.
    schemaRegistryClient: Optional<SchemaRegistryClient>
) : EventStore {

  @Value("\${block-modifying-operations:false}") private val blockModifyingOperations = false

  // Don't use KafkaAvroSerializer because it cannot be mocked
  private lateinit var kafkaAvroSerializer: Serializer<Any>

  init {
    // Workaround since configuration as a bean is currently not working. See:
    // https://github.com/confluentinc/schema-registry/issues/553
    kafkaAvroSerializer =
        if (environment.acceptsProfiles(Profiles.of("test"))) {
          KafkaAvroSerializer(schemaRegistryClient.get(), kafkaProperties.properties)
        } else {
          KafkaAvroSerializer(schemaRegistryClient.orElse(null), kafkaProperties.properties)
        }
  }

  @Transactional(propagation = MANDATORY)
  override fun save(kafkaStreamable: KafkaStreamable) {
    assertModifyingOperationsNotBlocked()

    val key = kafkaStreamable.toMessageKey()
    val value = kafkaStreamable.toAvroMessage()
    val tombstoneMessageKeys = kafkaStreamable.toAvroTombstoneMessageKeys()

    val topic = kafkaTopicProperties.getTopicForChannel(kafkaStreamable.getChannel())

    val tracer = GlobalTracer.get()
    val parentSpan = tracer.activeSpan()
    val span =
        tracer
            .buildSpan("send to kafka connector")
            .withTag("kafka.topic", topic)
            .asChildOf(parentSpan)
            .start()

    try {
      tracer.activateSpan(span).use {
        if (tombstoneMessageKeys.isNotEmpty()) {
          for (messageKey in tombstoneMessageKeys) {

            val eventStoreEntry =
                kafkaStreamable.toEvent(
                    kafkaAvroSerializer.serialize(topic, messageKey.toAvro()),
                    null,
                    partitionOf(messageKey, kafkaStreamable.getChannel()),
                    currentBusinessTransactionId())

            // Add the trace ID and the header name for kafka to use in the event store table
            eventStoreEntry.traceHeaderKey = "b3"
            eventStoreEntry.traceHeaderValue = toB3SingleHeader(span, parentSpan)
            em.persist(eventStoreEntry)
          }
        } else {
          // Serialize event and save it in the database
          val eventStoreEntry =
              kafkaStreamable.toEvent(
                  kafkaAvroSerializer.serialize(topic, key.toAvro()),
                  kafkaAvroSerializer.serialize(topic, value),
                  partitionOf(key, kafkaStreamable.getChannel()),
                  currentBusinessTransactionId())

          // Add the trace ID and the header name for kafka to use in the event store table
          eventStoreEntry.traceHeaderKey = "b3"
          eventStoreEntry.traceHeaderValue = toB3SingleHeader(span, parentSpan)
          em.persist(eventStoreEntry)
        }
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      markAsFailed(span, e)
      throw e
    } finally {
      span.finish()
    }
  }

  private fun partitionOf(key: EventMessageKey, channel: String): Int =
      toPositive(murmur2(key.rootContextIdentifier.toString().toByteArray(UTF_8))) %
          kafkaTopicProperties.getConfigForChannel(channel).partitions

  private fun toB3SingleHeader(currentSpan: Span, parentSpan: Span): String {
    val traceId = currentSpan.context().toTraceId()
    val spanId = currentSpan.context().toSpanId()
    val parentSpanId = parentSpan.context().toSpanId()

    // construct B3 single header format: {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
    return "$traceId-$spanId-$B3_SAMPLING_STATE_ACCEPT-$parentSpanId"
  }

  private fun markAsFailed(span: Span, ex: Exception) {
    span.setTag(Tags.ERROR, true)
    span.log(mapOf(ERROR_OBJECT to ex))
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
