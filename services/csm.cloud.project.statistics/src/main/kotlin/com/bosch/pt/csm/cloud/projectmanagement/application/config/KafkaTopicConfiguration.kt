/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.config

import java.util.HashMap
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
@ConfigurationProperties(prefix = "custom.kafka")
class KafkaTopicConfiguration {

  val bindings: Map<String, Binding> = HashMap()

  /**
   * Get the topic name for a given channel name.
   *
   * @param channel the logical channel
   * @return the kafka topic establishing the specified channel
   */
  fun getTopicForChannel(channel: String): String? {
    return if (bindings[channel] != null && bindings[channel]!!.kafkaTopic != null) {
      bindings[channel]!!.kafkaTopic
    } else {
      null
    }
  }

  /** A binding defines the kafka topic for a logical channel */
  class Binding {
    var kafkaTopic: String? = null
  }
}
