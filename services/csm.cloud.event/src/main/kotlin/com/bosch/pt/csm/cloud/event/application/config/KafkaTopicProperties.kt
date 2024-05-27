/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Configuration
@ConfigurationProperties(prefix = "custom.kafka")
@Validated
class KafkaTopicProperties {

  /** Binding for each context. */
  @field:Valid @field:NotEmpty var bindings: MutableMap<String, Binding> = HashMap()

  fun getTopicForChannel(channel: String): String =
      bindings
          .computeIfAbsent(channel) {
            throw IllegalArgumentException("No Kafka topic specified for channel $it")
          }
          .kafkaTopic!!

  fun getConfigForChannel(channel: String): TopicConfig =
      bindings
          .computeIfAbsent(channel) {
            throw IllegalArgumentException("No Kafka topic specified for channel $it")
          }
          .configuration

  /** A binding defines the kafka topic for a logical channel */
  class Binding {
    @field:NotBlank var kafkaTopic: String? = null

    /** Configuration applied to this topic. */
    var configuration = TopicConfig()
  }

  class TopicConfig {
    var partitions = 3
    var replication: Short = 3
    var properties: Map<String, String> = HashMap()
  }

  companion object {
    const val EVENT_BINDING = "event"
  }
}
