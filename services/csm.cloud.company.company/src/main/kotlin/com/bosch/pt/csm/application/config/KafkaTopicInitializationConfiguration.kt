/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

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
  @Profile("init-topics")
  fun admin(kafkaProperties: KafkaProperties): KafkaAdmin {
    val configs = kafkaProperties.buildAdminProperties()
    return KafkaAdmin(filterConfig(configs, AdminClientConfig.configNames()))
  }

  @Bean
  @Profile("init-topics")
  fun createCompanyTopic(kafkaTopicProperties: KafkaTopicProperties): NewTopic {
    val topicConfig = kafkaTopicProperties.getConfigForChannel(KafkaTopicProperties.COMPANY_BINDING)
    return NewTopic(
            kafkaTopicProperties.getTopicForChannel(KafkaTopicProperties.COMPANY_BINDING),
            topicConfig.partitions,
            topicConfig.replication)
        .configs(topicConfig.properties)
  }

  fun filterConfig(config: Map<String, Any>, configNames: Set<String>): Map<String, Any> =
      config.entries.filter { (key) -> configNames.contains(key) }.associate { it.key to it.value }
}
