/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common

import com.bosch.pt.csm.cloud.common.test.DefaultTimeLineGenerator
import java.time.Instant
import org.springframework.stereotype.Component

@Component
class TimeLineGeneratorImpl : DefaultTimeLineGenerator() {

  fun next(millis: Long = 5000): Instant {
    this.time = this.time.plusMillis(millis)
    return this.time
  }

  fun relativeToPrevious(millis: Long = 50): Instant = time.plusMillis(millis)
}
