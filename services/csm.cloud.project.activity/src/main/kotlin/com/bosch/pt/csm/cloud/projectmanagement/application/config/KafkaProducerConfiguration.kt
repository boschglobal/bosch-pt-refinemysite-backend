/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.MicrometerProducerListener

@Profile("!test")
@Configuration
class KafkaProducerConfiguration {

  @Bean
  fun kafkaProducerFactory(kafkaProperties: KafkaProperties, meterRegistry: MeterRegistry) =
      DefaultKafkaProducerFactory<Any, Any>(
              kafkaProperties.buildProducerProperties().filter {
                ProducerConfig.configNames().contains(it.key)
              })
          .apply { addListener(MicrometerProducerListener(meterRegistry)) }
}
