/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler

import com.bosch.pt.csm.cloud.common.eventstore.AbstractEventStore
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import jakarta.persistence.EntityManager
import java.util.Optional
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment

class TestEventStore(
    em: EntityManager,
    kafkaProperties: KafkaProperties,
    environment: Environment,
    schemaRegistryClient: Optional<SchemaRegistryClient>
) :
    AbstractEventStore<TestKafkaEvent>(
        false, em, kafkaProperties, environment, schemaRegistryClient) {

  override fun getNameOfEventTopic(): String {
    throw NotImplementedError()
  }

  override fun getNumberOfPartitionsOfEventTopic(): Int {
    throw NotImplementedError()
  }

  override fun createKafkaEvent(
      traceHeaderKey: String,
      traceHeaderValue: String,
      partitionNumber: Int,
      eventKey: ByteArray,
      event: ByteArray?
  ): TestKafkaEvent {
    throw NotImplementedError()
  }
}
