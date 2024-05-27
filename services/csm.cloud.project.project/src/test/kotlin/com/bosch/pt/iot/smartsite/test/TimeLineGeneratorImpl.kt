/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.iot.smartsite.test

import com.bosch.pt.csm.cloud.common.test.DefaultTimeLineGenerator
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Component

@Component
class TimeLineGeneratorImpl : DefaultTimeLineGenerator() {

  fun resetAtDaysAgo(daysAgo: Long) {
    time = Instant.now().minus(daysAgo, ChronoUnit.DAYS)
  }

  fun next(millis: Long = 5000): Instant {
    this.time = this.time.plusMillis(millis)
    return this.time
  }

  fun relativeToPrevious(millis: Long = 50): Instant = time.plusMillis(millis)
}
