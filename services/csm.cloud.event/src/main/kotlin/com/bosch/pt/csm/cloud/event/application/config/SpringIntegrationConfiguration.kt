package com.bosch.pt.csm.cloud.event.application.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
class SpringIntegrationConfiguration {

  @Bean fun broadcastingChannel(): PublishSubscribeChannel = PublishSubscribeChannel()

  @Bean fun heartbeatChannel(): PublishSubscribeChannel = PublishSubscribeChannel()

  @Scheduled(fixedDelay = 5000L)
  fun generateHeartBeat() {
    LOGGER.debug("Heartbeat generated")
    heartbeatChannel().send(MessageBuilder.withPayload("heartbeat").build())
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(SpringIntegrationConfiguration::class.java)
  }
}
