/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.ONLINE
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.RESTORE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException

class EventVersionValidatorTest {

  @Nested
  inner class `In online mode` {

    val cut = EventVersionValidator

    @Test
    fun `throws exception when version from snapshot is more than one less than version from event`() {
      assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
        cut.canApply(1, 3, ONLINE)
      }
    }

    @Test
    fun `returns true when version from snapshot is one less than version from event`() {
      assertThat(cut.canApply(2, 3, ONLINE)).isTrue
    }

    @Test
    fun `throws exception when version from snapshot equals version from event version`() {
      assertThatExceptionOfType(OptimisticLockingFailureException::class.java).isThrownBy {
        cut.canApply(3, 3, ONLINE)
      }
    }

    @Test
    fun `throws exception when version from snapshot is larger than version from event version`() {
      assertThatExceptionOfType(OptimisticLockingFailureException::class.java).isThrownBy {
        cut.canApply(4, 3, ONLINE)
      }
    }
  }

  @Nested
  inner class `In restore mode` {

    val cut = EventVersionValidator

    @Test
    fun `throws exception when version from snapshot is more than one less than version from event`() {
      assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
        cut.canApply(1, 3, RESTORE)
      }
    }

    @Test
    fun `returns true when version from snapshot is one less than version from event`() {
      assertThat(cut.canApply(2, 3, RESTORE)).isTrue
    }

    @Test
    fun `returns false when version from snapshot equals version from event version`() {
      assertThat(cut.canApply(3, 3, RESTORE)).isFalse
    }

    @Test
    fun `returns false when version from snapshot is larger than version from event version`() {
      assertThat(cut.canApply(4, 3, RESTORE)).isFalse
    }
  }
}
