/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.config

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

  var bindings: @Valid @NotEmpty MutableMap<String, Binding> = HashMap()

  /** A binding defines the kafka topic for a logical channel */
  class Binding(val kafkaTopic: @NotBlank String, val configuration: TopicConfig = TopicConfig())

  class TopicConfig(
      val partitions: Int = 1,
      val replication: Short = 3,
      val properties: Map<String, String> = HashMap(),
  )

  fun getTopicForChannel(channel: String): String = getBindingForChanel(channel).kafkaTopic

  fun getConfigForChannel(channel: String): TopicConfig = getBindingForChanel(channel).configuration

  private fun getBindingForChanel(channel: String) =
      bindings.computeIfAbsent(channel) {
        throw IllegalArgumentException("No Kafka topic specified for channel $it")
      }

  companion object {
    const val FEATURETOGGLE_BINDING = "featuretoggle"
  }
}
