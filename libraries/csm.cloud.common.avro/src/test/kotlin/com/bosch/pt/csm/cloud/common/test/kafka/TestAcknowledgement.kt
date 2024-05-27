/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.test.kafka

import org.springframework.kafka.support.Acknowledgment

class TestAcknowledgement : Acknowledgment {
  var isAcknowledged = false
    private set

  override fun acknowledge() {
    isAcknowledged = true
  }
}
