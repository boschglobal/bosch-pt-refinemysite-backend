/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.kafka

import org.springframework.kafka.support.Acknowledgment

@Deprecated("Use from csm.cloud.common.avro")
class TestAcknowledgement : Acknowledgment {

  var isAcknowledged = false
    private set

  override fun acknowledge() {
    isAcknowledged = true
  }
}
