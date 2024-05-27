/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.config

import com.bosch.pt.csm.cloud.event.application.config.KafkaTopicProperties.Companion.EVENT_BINDING
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaAdmin

@Configuration
@Profile("init-topics")
class KafkaTopicInitializationConfiguration {

  @Bean
  fun admin(kafkaProperties: KafkaProperties): KafkaAdmin =
      KafkaAdmin(
          filterConfig(kafkaProperties.buildAdminProperties(), AdminClientConfig.configNames()))

  @Bean
  fun createEventTopic(kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      createTopic(EVENT_BINDING, kafkaTopicProperties)

  private fun createTopic(channel: String, kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      kafkaTopicProperties.getConfigForChannel(channel).let {
        NewTopic(kafkaTopicProperties.getTopicForChannel(channel), it.partitions, it.replication)
            .configs(it.properties)
      }

  fun filterConfig(config: Map<String, Any?>, configNames: Set<String?>): Map<String, Any?> =
      config.entries.filter { configNames.contains(it.key) }.associate { it.key to it.value }
}
