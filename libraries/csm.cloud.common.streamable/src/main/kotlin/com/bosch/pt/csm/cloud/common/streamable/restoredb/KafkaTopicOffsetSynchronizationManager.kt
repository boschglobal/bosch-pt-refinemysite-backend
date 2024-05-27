/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.restoredb

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutionException
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.scheduling.annotation.Scheduled

class KafkaTopicOffsetSynchronizationManager(
    kafkaProperties: KafkaProperties,
    private val consumerGroupName: String
) : OffsetSynchronizationManager {

  private val topicPartitionOffsets: ConcurrentMap<String, ConcurrentMap<Int, Long>> =
      ConcurrentHashMap()

  private val adminClient: AdminClient

  init {
    adminClient = AdminClient.create(kafkaProperties.buildAdminProperties())
  }

  @Scheduled(fixedDelay = 10000L)
  @Throws(ExecutionException::class, InterruptedException::class)
  fun updateScheduled() {
    val consumerGroupOffsets = adminClient.listConsumerGroupOffsets(consumerGroupName)
    val offsetAndMetadataMap = consumerGroupOffsets.partitionsToOffsetAndMetadata().get()

    offsetAndMetadataMap.forEach {
        (topicPartition: TopicPartition, offsetAndMetadata: OffsetAndMetadata) ->
      var offsetMapping = topicPartitionOffsets[topicPartition.topic()]
      if (offsetMapping == null) {
        offsetMapping = ConcurrentHashMap()
        topicPartitionOffsets[topicPartition.topic()] = offsetMapping
      }

      offsetMapping[topicPartition.partition()] = offsetAndMetadata.offset()
    }
  }

  override fun getMaxTopicPartitionOffset(topic: String, partition: Int): Long? =
      topicPartitionOffsets[topic]?.get(partition)
}
