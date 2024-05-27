/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.listener

import com.bosch.pt.csm.cloud.common.streamable.restoredb.OffsetSynchronizationManager

class NoOpOffsetSynchronizationManager : OffsetSynchronizationManager {

  override fun getMaxTopicPartitionOffset(topic: String, partition: Int): Long = Long.MAX_VALUE
}
