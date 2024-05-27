/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.config

import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.MicrometerProducerListener
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaProducerConfiguration {

  @Bean
  fun kafkaProducerFactory(
      kafkaProperties: KafkaProperties,
      meterRegistry: MeterRegistry
  ): ProducerFactory<Any, Any?> =
      kafkaProperties.buildProducerProperties().let {
        DefaultKafkaProducerFactory<Any, Any?>(filterConfig(it, ProducerConfig.configNames()))
            .apply { addListener(MicrometerProducerListener(meterRegistry)) }
      }

  private fun filterConfig(config: Map<String, Any?>, configNames: Set<String>): Map<String, Any?> =
      config.entries.filter { configNames.contains(it.key) }.associate { it.key to it.value }
}
