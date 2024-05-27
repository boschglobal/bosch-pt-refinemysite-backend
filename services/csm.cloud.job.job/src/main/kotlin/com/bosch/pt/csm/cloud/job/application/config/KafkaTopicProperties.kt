/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.job.application.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "custom.kafka")
@Validated
data class KafkaTopicProperties(val bindings: @Valid @NotEmpty Map<String, Binding>) {

  /** A binding defines the kafka topic for a logical channel */
  data class Binding(val kafkaTopic: @NotBlank String, val configuration: TopicConfig = TopicConfig())

  data class TopicConfig(
      val partitions: Int = 3,
      val replication: Short = 3,
      val properties: Map<String, String> = emptyMap(),
  )

  fun getTopicForChannel(channel: String): String = getBindingForChannel(channel).kafkaTopic

  fun getConfigForChannel(channel: String): TopicConfig =
      getBindingForChannel(channel).configuration

  private fun getBindingForChannel(channel: String) =
      bindings[channel]
          ?: throw IllegalArgumentException("No Kafka topic specified for channel $channel")

  companion object {
    const val JOB_COMMAND = "job-command"
    const val JOB_EVENT = "job-event"
  }
}
