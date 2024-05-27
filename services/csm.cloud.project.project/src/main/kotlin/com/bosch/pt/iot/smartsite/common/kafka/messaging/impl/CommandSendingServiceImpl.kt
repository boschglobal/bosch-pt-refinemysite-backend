/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.messaging.impl

import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_WRITING_OPERATIONS
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import io.confluent.kafka.serializers.KafkaAvroSerializer
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.ExecutionException
import jakarta.annotation.PostConstruct
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.utils.Utils.murmur2
import org.apache.kafka.common.utils.Utils.toPositive
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Profile("!test & !restore-db")
@Service
class CommandSendingServiceImpl(
    private val kafkaProperties: KafkaProperties,
    private val kafkaTopicProperties: KafkaTopicProperties,
    private val kafkaTemplate: KafkaTemplate<ByteArray, ByteArray>
) : CommandSendingService {

  // Don't use KafkaAvroSerializer because it cannot be mocked
  private lateinit var kafkaAvroSerializer: Serializer<Any>

  @Value("\${block-modifying-operations:false}") private val blockModifyingOperations = false

  @PostConstruct
  fun instantiateKafkaAvroSerializer() {
    // Workaround since configuration as a bean is currently not working. See:
    // https://github.com/confluentinc/schema-registry/issues/553
    kafkaAvroSerializer = KafkaAvroSerializer(null, kafkaProperties.properties)
  }

  override fun send(key: CommandMessageKey, value: SpecificRecord, channel: String) {
    send(key.toAvro(), value, channel, partitionOf(key, channel))
  }

  private fun send(key: SpecificRecord, value: SpecificRecord, channel: String, partition: Int) {
    assertModifyingOperationsNotBlocked()

    val topic = kafkaTopicProperties.getTopicForChannel(channel)
    val producerRecord =
        ProducerRecord(
            topic,
            partition,
            kafkaAvroSerializer.serialize(topic, key),
            kafkaAvroSerializer.serialize(topic, value))

    try {
      kafkaTemplate.send(producerRecord).get()
    } catch (e: InterruptedException) {
      throw IllegalStateException(e)
    } catch (e: ExecutionException) {
      throw IllegalStateException(e)
    }
  }

  private fun partitionOf(keyAvro: EventMessageKey, channel: String): Int =
      toPositive(murmur2(keyAvro.rootContextIdentifier.toString().toByteArray(UTF_8))) %
          kafkaTopicProperties.getConfigForChannel(channel).partitions

  private fun partitionOf(keyAvro: CommandMessageKey, channel: String): Int =
      toPositive(murmur2(keyAvro.partitioningIdentifier.toString().toByteArray(UTF_8))) %
          kafkaTopicProperties.getConfigForChannel(channel).partitions

  private fun assertModifyingOperationsNotBlocked() {
    if (blockModifyingOperations) {
      throw BlockOperationsException(BLOCK_WRITING_OPERATIONS)
    }
  }
}
