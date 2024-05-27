/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_DELETE_BINDING
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_INVITATION_BINDING
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaAdmin

@Configuration
@Profile("init-topics")
open class KafkaTopicInitializationConfiguration {

  @Bean
  open fun admin(kafkaProperties: KafkaProperties): KafkaAdmin =
      KafkaAdmin(
          filterConfig(kafkaProperties.buildAdminProperties(), AdminClientConfig.configNames()))

  @Bean
  open fun createProjectTopic(kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      createTopic(PROJECT_BINDING, kafkaTopicProperties)

  @Bean
  open fun createProjectDeleteTopic(kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      createTopic(PROJECT_DELETE_BINDING, kafkaTopicProperties)

  @Bean
  open fun createProjectInvitationTopic(kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      createTopic(PROJECT_INVITATION_BINDING, kafkaTopicProperties)

  private fun createTopic(channel: String, kafkaTopicProperties: KafkaTopicProperties): NewTopic =
      with(kafkaTopicProperties.getConfigForChannel(channel)) {
        NewTopic(
                kafkaTopicProperties.getTopicForChannel(channel), this.partitions, this.replication)
            .configs(this.properties)
      }

  private fun filterConfig(config: Map<String, Any?>, configNames: Set<String>): Map<String, Any?> =
      config.entries.filter { configNames.contains(it.key) }.associate { it.key to it.value }
}
