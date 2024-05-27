/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.application.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.MicrometerProducerListener
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@ConfigurationProperties("spring.kafka.avro-producer")
class KafkaProducerAvroConfiguration : KafkaProperties.Producer() {

  @Bean
  @Qualifier("avro")
  fun avroProducerFactory(
      kafkaProperties: KafkaProperties,
      meterRegistry: MeterRegistry
  ): ProducerFactory<Any, Any> {
    val explicitlyConfiguredTransactionIdPrefix = this.transactionIdPrefix
    require(!explicitlyConfiguredTransactionIdPrefix.isNullOrBlank())
    val properties = kafkaProperties.buildProducerProperties(null) + this.buildProperties(null)
    return DefaultKafkaProducerFactory<Any, Any>(properties).apply {
      transactionIdPrefix = explicitlyConfiguredTransactionIdPrefix
      addListener(MicrometerProducerListener(meterRegistry))
    }
  }

  @Bean
  @Qualifier("avro")
  fun avroKafkaTemplate(avroProducerFactory: ProducerFactory<Any, Any>): KafkaTemplate<*, *> =
      KafkaTemplate(avroProducerFactory)
}

@Configuration
@ConfigurationProperties("spring.kafka.json-producer")
class KafkaProducerJsonConfiguration : KafkaProperties.Producer() {

  @Bean
  @Qualifier("json")
  fun jsonProducerFactory(
      kafkaProperties: KafkaProperties,
      meterRegistry: MeterRegistry,
      objectMapper: ObjectMapper
  ): ProducerFactory<String, Any> {
    val properties =
        filterConfig(kafkaProperties.buildProducerProperties(null) + this.buildProperties(null))
    return DefaultKafkaProducerFactory<String, Any>(properties).apply {
      addListener(MicrometerProducerListener(meterRegistry))
      keySerializer = StringSerializer()
      setValueSerializer(JsonSerializer(objectMapper))
    }
  }

  @Bean
  @Qualifier("json")
  fun jsonKafkaTemplate(
      jsonProducerFactory: ProducerFactory<String, Any>,
  ): KafkaTemplate<*, *> = KafkaTemplate(jsonProducerFactory)

  // Avoids warning messages when bootstrapping the json kafka producer due to unknown properties.
  private fun filterConfig(config: Map<String, Any?>): Map<String, Any?> =
      config.entries
          .filter { ProducerConfig.configNames().contains(it.key) }
          .filter { it.key != "internal.auto.downgrade.txn.commit" }
          .associate { it.key to it.value }
}
