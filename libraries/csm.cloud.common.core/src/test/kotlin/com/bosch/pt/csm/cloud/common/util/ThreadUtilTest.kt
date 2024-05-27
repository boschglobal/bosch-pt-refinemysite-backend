/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.util

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class ThreadUtilTest {

  @Test
  fun `exception in procedure is rethrown to caller thread`() {
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy {
          ThreadUtil.runAsThreadAndWaitForCompletion(100) { throw IllegalStateException("test") }
        }
        .withMessage("test")
  }

  @Test
  fun `throws exception if thread is still alive after timeout`() {
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { ThreadUtil.runAsThreadAndWaitForCompletion(100) { Thread.sleep(200) } }
        .withMessageContaining("The thread is still alive after waiting")
  }
}
