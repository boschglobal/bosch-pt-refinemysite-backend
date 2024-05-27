/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.consents.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractEventStore
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties.Companion.CONSENTS_BINDING
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import java.util.Optional
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class ConsentsContextEventStore(
    private val kafkaTopicProperties: KafkaTopicProperties,
    @Value("\${block-modifying-operations:false}") blockModifyingOperations: Boolean = false,
    em: EntityManager,
    kafkaProperties: KafkaProperties,
    environment: Environment,
    schemaRegistryClient: Optional<SchemaRegistryClient>
) :
    AbstractEventStore<ConsentsContextKafkaEvent>(
        blockModifyingOperations, em, kafkaProperties, environment, schemaRegistryClient) {

  override fun getNameOfEventTopic() = kafkaTopicProperties.getTopicForChannel(CONSENTS_BINDING)

  override fun getNumberOfPartitionsOfEventTopic() =
      kafkaTopicProperties.getConfigForChannel(CONSENTS_BINDING).partitions

  override fun createKafkaEvent(
      traceHeaderKey: String,
      traceHeaderValue: String,
      partitionNumber: Int,
      eventKey: ByteArray,
      event: ByteArray?
  ): ConsentsContextKafkaEvent =
      ConsentsContextKafkaEvent(traceHeaderKey, traceHeaderValue, partitionNumber, eventKey, event)
}
