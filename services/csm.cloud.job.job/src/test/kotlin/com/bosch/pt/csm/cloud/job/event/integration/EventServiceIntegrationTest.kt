/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.event.integration

import com.bosch.pt.csm.cloud.job.application.config.KafkaProducerJsonConfiguration
import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.extensions.KafkaTestExtension
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
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

@Import(EventService::class, KafkaProducerJsonConfiguration::class, LoggerConfiguration::class)
@ImportAutoConfiguration(KafkaAutoConfiguration::class, JacksonAutoConfiguration::class)
private class EventServiceIntegrationTestConfiguration

@ExtendWith(KafkaTestExtension::class)
@ActiveProfiles("local")
@SpringBootTest(classes = [EventServiceIntegrationTestConfiguration::class])
class EventServiceIntegrationTest {

  @MockkBean(relaxed = true) lateinit var meterRegistry: MeterRegistry

  @Value("\${custom.kafka.bindings.event.kafkaTopic}") private lateinit var topic: String

  @Autowired lateinit var kafkaProperties: KafkaProperties

  @Autowired lateinit var eventService: EventService

  @Test
  fun `publishes to Event service`() {
    val consumerFactory =
        DefaultKafkaConsumerFactory(
            kafkaProperties.buildConsumerProperties(), StringDeserializer(), StringDeserializer())
    val container = KafkaMessageListenerContainer(consumerFactory, ContainerProperties(topic))
    val records = LinkedBlockingQueue<ConsumerRecord<String, String>>()
    val latch = CountDownLatch(1)
    container.setupMessageListener(
        MessageListener<String, String> { data ->
          records.add(data)
          latch.countDown()
        })
    container.start()

    eventService.send(
        UserIdentifier("253b90dd-9feb-bb07-35cb-7c55a2c37d58"), TestMessage("TestMessage"))

    latch.await(10, SECONDS)
    assertThat(records.single().value())
        .isEqualToIgnoringWhitespace(
            """{
                "receivers": ["253b90dd-9feb-bb07-35cb-7c55a2c37d58"],
                "eventType": "job",
                "message": {
                    "msg": "TestMessage"
                }
            }""")
  }
}

private data class TestMessage(val msg: String)
