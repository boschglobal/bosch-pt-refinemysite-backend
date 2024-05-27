/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "custom.kafka")
class KafkaTopicConfiguration {

  val bindings: Map<String, Binding?> = HashMap()

  /**
   * @param channel the logical channel
   * @return the Kafka topic establishing the specified channel
   */
  fun getTopicForChannel(channel: String): String? = bindings[channel]?.kafkaTopic

  /** A binding defines the kafka topic for a logical channel */
  class Binding {
    var kafkaTopic: String? = null
  }
}
