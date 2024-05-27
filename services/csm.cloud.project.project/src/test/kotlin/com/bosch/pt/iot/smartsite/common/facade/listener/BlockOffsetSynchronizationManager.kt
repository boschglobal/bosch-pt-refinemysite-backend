/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.listener

import com.bosch.pt.csm.cloud.common.streamable.restoredb.OffsetSynchronizationManager

class BlockOffsetSynchronizationManager : OffsetSynchronizationManager {

  private val topicPartitionOffsets: MutableMap<String, MutableMap<Int, Long>> = HashMap()

  fun setMaxTopicPartitionOffset(topic: String, partition: Int, offset: Long) {
    val partitionOffsets = topicPartitionOffsets.computeIfAbsent(topic) { HashMap() }
    partitionOffsets[partition] = offset
  }

  override fun getMaxTopicPartitionOffset(topic: String, partition: Int): Long? {
    val partitionOffsets = topicPartitionOffsets[topic] ?: return null
    return partitionOffsets[partition]!!
  }
}
