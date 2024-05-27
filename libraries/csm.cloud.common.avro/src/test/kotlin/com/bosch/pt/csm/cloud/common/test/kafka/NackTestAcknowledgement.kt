/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.test.kafka

import java.time.Duration
import org.springframework.kafka.support.Acknowledgment

class NackTestAcknowledgement : Acknowledgment {
  var isAcknowledged = false
    private set
  var isNotAcknowledged = false
    private set

  override fun acknowledge() {
    isAcknowledged = true
  }

  override fun nack(sleep: Duration) {
    isNotAcknowledged = true
  }
}
