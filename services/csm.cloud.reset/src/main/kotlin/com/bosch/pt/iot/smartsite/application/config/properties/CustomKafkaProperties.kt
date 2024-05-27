/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Configuration
@EnableConfigurationProperties(CustomKafkaProperties::class)
class CustomKafkaPropertiesConfiguration

@ConfigurationProperties(prefix = "smartsite.kafka")
@Validated
class CustomKafkaProperties(
    @Valid @NotEmpty val bindings: MutableMap<String, Binding> = HashMap()
) {

  /** A binding defines the kafka topic for a logical channel */
  class Binding(
      @NotBlank val kafkaTopic: String,
      @NotBlank val kafkaTopicPrefix: String,
      val configuration: TopicConfig = TopicConfig()
  )

  class TopicConfig(
      val partitions: Int = 3,
      val replication: Short = 3,
      var properties: Map<String, String> = emptyMap()
  )

  fun getTopicForChannel(channel: String): String =
      bindings
          .computeIfAbsent(channel) {
            throw IllegalArgumentException("No Kafka topic specified for channel $it")
          }
          .kafkaTopic

  fun getTopicPrefixes(): List<String> = bindings.values.map { it.kafkaTopicPrefix }

  companion object {
    const val USER_BINDING = "user"
  }
}
