/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaConfiguration(val kafkaProperties: KafkaProperties) {

  @Bean
  fun cachedSchemaRegistryClient(): SchemaRegistryClient =
      KafkaAvroSerializerConfig(kafkaProperties.properties).let {
        CachedSchemaRegistryClient(
            it.schemaRegistryUrls, it.maxSchemasPerSubject, it.originalsWithPrefix(""))
      }
}
