package com.bosch.pt.csm.cloud.event.event.facade.listener

import datadog.trace.api.Trace
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Profile("!test")
@Component
class EventListener(
    @Qualifier("broadcastingChannel") private val broadcastingChannel: MessageChannel
) {

  @Trace
  @KafkaListener(topics = ["#{kafkaTopicProperties.getTopicForChannel('event')}"])
  fun listenToEvents(record: ConsumerRecord<String, Any>, ack: Acknowledgment) {
    log(record)
    broadcastingChannel.send(MessageBuilder.withPayload(record.value()).build())
    ack.acknowledge()
  }

  private fun log(record: ConsumerRecord<String, Any>) {
    LOGGER.debug(
        "Processing message for topic {} partition {} and offset {}",
        record.topic(),
        record.partition(),
        record.offset())
  }

  // workaround to avoid illegal reflection access warning
  // by spring proxies (due to java 11)
  override fun toString(): String =
      this.javaClass.getName() + "@" + Integer.toHexString(this.hashCode())

  companion object {
    private val LOGGER = LoggerFactory.getLogger(EventListener::class.java)
  }
}
