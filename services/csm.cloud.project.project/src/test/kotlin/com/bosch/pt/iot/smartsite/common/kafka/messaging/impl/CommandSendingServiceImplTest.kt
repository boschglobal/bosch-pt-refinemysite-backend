/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.messaging.impl

import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.common.kafka.serializer.KafkaAvroTestSerializer
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.util.ReflectionTestUtils

@SmartSiteMockKTest
class CommandSendingServiceImplTest {

  @Suppress("UnusedPrivateMember")
  @RelaxedMockK
  private lateinit var kafkaProperties: KafkaProperties

  @RelaxedMockK private lateinit var kafkaTopicProperties: KafkaTopicProperties

  @RelaxedMockK private lateinit var kafkaTemplate: KafkaTemplate<ByteArray, ByteArray>

  @InjectMockKs private lateinit var cut: CommandSendingServiceImpl

  @Test
  fun verifyBlocking() {

    // Enable blocking
    ReflectionTestUtils.setField(cut, "blockModifyingOperations", true)

    mockKafkaProperties()

    // Ensure that message cannot be sent
    assertThatExceptionOfType(BlockOperationsException::class.java).isThrownBy {
      cut.send(buildKey(), buildValue(), "test")
    }
  }

  @Test
  fun verifyNotBlocking() {

    // Disable blocking and set serializer
    ReflectionTestUtils.setField(cut, "blockModifyingOperations", false)
    ReflectionTestUtils.setField(cut, "kafkaAvroSerializer", KafkaAvroTestSerializer())

    // Mock kafka properties and template
    mockKafkaProperties()
    mockKafkaTemplate()

    // Send message
    cut.send(buildKey(), buildValue(), "test")

    // Check mocks
    verify { kafkaTemplate.send(any() as ProducerRecord<ByteArray, ByteArray>) }

    confirmVerified(kafkaTemplate)
  }

  private fun buildKey() = CommandMessageKey(UUID.randomUUID())

  private fun buildValue(): MessageEventAvro =
      MessageEventAvro.newBuilder()
          .setAggregateBuilder(
              MessageAggregateAvro.newBuilder()
                  .setAggregateIdentifierBuilder(aggregateIdentifier)
                  .setAuditingInformationBuilder(
                      AuditingInformationAvro.newBuilder()
                          .setCreatedByBuilder(aggregateIdentifier)
                          .setLastModifiedByBuilder(aggregateIdentifier)
                          .setCreatedDate(1L)
                          .setLastModifiedDate(1L))
                  .setContent("content")
                  .setTopicBuilder(aggregateIdentifier))
          .setName(CREATED)
          .build()

  private fun mockKafkaProperties() {
    every { kafkaTopicProperties.getConfigForChannel(any()) } returns
        KafkaTopicProperties.TopicConfig()
    every { kafkaTopicProperties.getTopicForChannel(any()) } returns "test-topic"
  }

  private fun mockKafkaTemplate() {
    every { kafkaTemplate.send(any() as ProducerRecord<ByteArray, ByteArray>) } returns mockk(relaxed = true)
  }

  private val aggregateIdentifier: AggregateIdentifierAvro.Builder
    get() =
        AggregateIdentifierAvro.newBuilder()
            .setIdentifier(randomUUID().toString())
            .setType("TYPE")
            .setVersion(1L)
}
