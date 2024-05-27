/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.event.listener

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

interface JobEventListener {

  fun listenToJobEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  )
}
