/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.config

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
      kafkaProperties.buildAdminProperties().let {
        KafkaAdmin(filterConfig(it, AdminClientConfig.configNames()))
      }

  @Bean
  fun createFeatureToggleTopic(kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      kafkaTopicProperties.getConfigForChannel(KafkaTopicProperties.FEATURETOGGLE_BINDING).let {
        NewTopic(
                kafkaTopicProperties.getTopicForChannel(KafkaTopicProperties.FEATURETOGGLE_BINDING),
                it.partitions,
                it.replication)
            .configs(it.properties)
      }

  fun filterConfig(config: Map<String, Any>, configNames: Set<String>): Map<String, Any> =
      config.entries.filter { (key) -> configNames.contains(key) }.associate { it.key to it.value }
}
