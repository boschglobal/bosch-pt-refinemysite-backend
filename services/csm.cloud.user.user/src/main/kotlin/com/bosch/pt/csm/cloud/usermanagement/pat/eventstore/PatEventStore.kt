/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractEventStore
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties.Companion.PAT_BINDING
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import jakarta.persistence.EntityManager
import java.util.Optional
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class PatEventStore(
    private val kafkaTopicProperties: KafkaTopicProperties,
    @Value("\${block-modifying-operations:false}") blockModifyingOperations: Boolean = false,
    em: EntityManager,
    kafkaProperties: KafkaProperties,
    environment: Environment,
    schemaRegistryClient: Optional<SchemaRegistryClient>
) :
    AbstractEventStore<PatKafkaEvent>(
        blockModifyingOperations, em, kafkaProperties, environment, schemaRegistryClient) {

  override fun getNameOfEventTopic() = kafkaTopicProperties.getTopicForChannel(PAT_BINDING)

  override fun getNumberOfPartitionsOfEventTopic() =
      kafkaTopicProperties.getConfigForChannel(PAT_BINDING).partitions

  override fun createKafkaEvent(
      traceHeaderKey: String,
      traceHeaderValue: String,
      partitionNumber: Int,
      eventKey: ByteArray,
      event: ByteArray?
  ): PatKafkaEvent =
      PatKafkaEvent(traceHeaderKey, traceHeaderValue, partitionNumber, eventKey, event)
}
