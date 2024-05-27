/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.featuretoggle.eventstore

import com.bosch.pt.csm.cloud.application.config.KafkaTopicProperties
import com.bosch.pt.csm.cloud.application.config.KafkaTopicProperties.Companion.FEATURETOGGLE_BINDING
import com.bosch.pt.csm.cloud.common.eventstore.AbstractEventStore
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import java.util.Optional
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class FeaturetoggleContextEventStore(
    private val kafkaTopicProperties: KafkaTopicProperties,
    @Value("\${block-modifying-operations:false}") blockModifyingOperations: Boolean = false,
    em: EntityManager,
    kafkaProperties: KafkaProperties,
    environment: Environment,
    schemaRegistryClient: Optional<SchemaRegistryClient>
) :
    AbstractEventStore<FeaturetoggleContextKafkaEvent>(
        blockModifyingOperations, em, kafkaProperties, environment, schemaRegistryClient) {

  override fun getNameOfEventTopic() =
      kafkaTopicProperties.getTopicForChannel(FEATURETOGGLE_BINDING)

  override fun getNumberOfPartitionsOfEventTopic() =
      kafkaTopicProperties.getConfigForChannel(FEATURETOGGLE_BINDING).partitions

  override fun createKafkaEvent(
      traceHeaderKey: String,
      traceHeaderValue: String,
      partitionNumber: Int,
      eventKey: ByteArray,
      event: ByteArray?
  ): FeaturetoggleContextKafkaEvent =
      FeaturetoggleContextKafkaEvent(
          traceHeaderKey, traceHeaderValue, partitionNumber, eventKey, event)
}
