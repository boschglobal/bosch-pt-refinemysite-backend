/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test

import java.time.Instant

interface TimeLineGenerator {

  /** Resets the time line generator */
  fun reset()

  /**
   * Returns the next instant in the time line (default delay).
   *
   * @return the next instant
   */
  fun next(): Instant

  /**
   * Returns the instant relative to the last value in the time line (with default delay) without
   * modifying the last value of the timeline.
   *
   * @return the instant relative to the last value (with default delay)
   */
  fun relativeToPrevious(): Instant
}
