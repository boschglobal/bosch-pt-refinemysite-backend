/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractEventStore
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties.Companion.USER_BINDING
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import jakarta.persistence.EntityManager
import java.util.Optional
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Kafka's max.message.bytes setting minus some kilobytes to factor in the message overhead. Let's
 * assume a generous overhead of 100 KB to really be sure that no message is sent to Kafka that
 * exceeds max.message.bytes.
 */
private const val MAX_MESSAGE_BYTES_MINUS_OVERHEAD = 900 * 1_024

@Component
class UserContextEventStore(
    private val kafkaTopicProperties: KafkaTopicProperties,
    @Value("\${block-modifying-operations:false}") blockModifyingOperations: Boolean = false,
    em: EntityManager,
    kafkaProperties: KafkaProperties,
    environment: Environment,
    schemaRegistryClient: Optional<SchemaRegistryClient>
) :
    AbstractEventStore<UserContextKafkaEvent>(
        blockModifyingOperations, em, kafkaProperties, environment, schemaRegistryClient) {

  override fun getNameOfEventTopic() = kafkaTopicProperties.getTopicForChannel(USER_BINDING)

  override fun getNumberOfPartitionsOfEventTopic() =
      kafkaTopicProperties.getConfigForChannel(USER_BINDING).partitions

  override fun createKafkaEvent(
      traceHeaderKey: String,
      traceHeaderValue: String,
      partitionNumber: Int,
      eventKey: ByteArray,
      event: ByteArray?
  ): UserContextKafkaEvent =
      UserContextKafkaEvent(traceHeaderKey, traceHeaderValue, partitionNumber, eventKey, event)
          .also {
            // TODO: [SMAR-19850] move message size validation to AbstractEventStore
            val estimatedMessageSizeInBytes = eventKey.size + (event?.size ?: 0)
            if (estimatedMessageSizeInBytes > MAX_MESSAGE_BYTES_MINUS_OVERHEAD) {
              throw IllegalStateException(
                  "The serialized event value + serialized event key must not exceed " +
                      "$MAX_MESSAGE_BYTES_MINUS_OVERHEAD bytes but is $estimatedMessageSizeInBytes.")
            }
          }
}
