package com.bosch.pt.csm.cloud.projectmanagement.event.boundary

import com.bosch.pt.csm.cloud.projectmanagement.application.config.KafkaProducerConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.event.integration.EventIntegrationService
import com.bosch.pt.csm.cloud.projectmanagement.extensions.KafkaTestExtension
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.MeterRegistry
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.test.context.ActiveProfiles

@ExtendWith(KafkaTestExtension::class)
@ActiveProfiles("local")
@SpringBootTest(classes = [EventServiceIntegrationTestConfiguration::class])
class EventServiceIntegrationTest {

  @Autowired lateinit var eventService: EventService

  @Autowired lateinit var kafkaProperties: KafkaProperties

  @Value("\${custom.kafka.bindings.event.kafkaTopic}") lateinit var eventTopic: String

  @MockkBean(relaxed = true) lateinit var meterRegistry: MeterRegistry

  @Test
  fun `sends events via Event service`() {
    val consumerFactory =
        DefaultKafkaConsumerFactory(
            kafkaProperties.buildConsumerProperties(), StringDeserializer(), StringDeserializer())
    val container = KafkaMessageListenerContainer(consumerFactory, ContainerProperties(eventTopic))
    val records = LinkedBlockingQueue<ConsumerRecord<String, String>>()
    val latch = CountDownLatch(1)
    container.setupMessageListener(
        MessageListener { data ->
          records.add(data)
          latch.countDown()
        })
    container.start()

    eventService.send(
        UUID.fromString("5af7b0f9-b161-4d30-bb65-27efb5ff5079"),
        Instant.parse("2023-05-31T08:18:21.311748301Z"))

    latch.await(10, TimeUnit.SECONDS)
    // language=JSON
    assertThat(records.single().value())
        .isEqualToIgnoringWhitespace(
            """{
                "receivers": [
                  "5af7b0f9-b161-4d30-bb65-27efb5ff5079"
                ],
                "eventType": "notification",
                "message": "{\"lastAdded\":\"2023-05-31T08:18:21.311748301Z\"}"
              }"""
                .trimIndent())
  }
}

@Import(EventService::class, EventIntegrationService::class, KafkaProducerConfiguration::class)
@ImportAutoConfiguration(KafkaAutoConfiguration::class, JacksonAutoConfiguration::class)
private class EventServiceIntegrationTestConfiguration
