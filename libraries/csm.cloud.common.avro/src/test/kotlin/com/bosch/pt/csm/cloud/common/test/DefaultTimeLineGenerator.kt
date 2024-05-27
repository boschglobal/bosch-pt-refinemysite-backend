/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test

import java.time.Instant

open class DefaultTimeLineGenerator : TimeLineGenerator {

  lateinit var time: Instant

  init {
    this.reset()
  }

  override fun reset() {
    time = Instant.now().minusSeconds(60 * 60 * 24)
  }

  override fun next(): Instant {
    this.time = this.time.plusMillis(5000)
    return this.time
  }

  override fun relativeToPrevious(): Instant = time.plusMillis(50)
}
