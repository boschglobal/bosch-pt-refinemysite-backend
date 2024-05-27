/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.restoredb

interface OffsetSynchronizationManager {

  fun getMaxTopicPartitionOffset(topic: String, partition: Int): Long?
}
