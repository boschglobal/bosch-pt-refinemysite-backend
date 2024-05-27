/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PpcCalculatorTest {

  @Test
  fun calculatePpc_equalDistribution() {
    val list =
        listOf(
            DayCardCountEntry(DayCardStatusEnum.APPROVED, 10L, 0),
            DayCardCountEntry(DayCardStatusEnum.DONE, 10L, 0),
            DayCardCountEntry(DayCardStatusEnum.NOTDONE, 10L, 0),
            DayCardCountEntry(DayCardStatusEnum.OPEN, 10L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(50L)
  }

  @Test
  fun calculatePpc_onlyApproved() {
    val list = listOf(DayCardCountEntry(DayCardStatusEnum.APPROVED, 200L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(100L)
  }

  @Test
  fun calculatePpc_onlyDone() {
    val list = listOf(DayCardCountEntry(DayCardStatusEnum.DONE, 25L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(100L)
  }

  @Test
  fun calculatePpc_onlyNotDone() {
    val list = listOf(DayCardCountEntry(DayCardStatusEnum.NOTDONE, 25L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(0L)
  }

  @Test
  fun calculatePpc_zero() {
    val list =
        listOf(
            DayCardCountEntry(DayCardStatusEnum.APPROVED, 0L, 0),
            DayCardCountEntry(DayCardStatusEnum.DONE, 0L, 0),
            DayCardCountEntry(DayCardStatusEnum.NOTDONE, 0L, 0),
            DayCardCountEntry(DayCardStatusEnum.OPEN, 0L, 0))

    assertThat(PpcCalculator(list).total()).isNull()
  }

  @Test
  fun calculatePpc_empty() {
    val list = emptyList<DayCardCountEntry>()

    assertThat(PpcCalculator(list).total()).isNull()
  }

  @Test
  fun calculatePpc_onlyIncomplete() {
    val list =
        listOf(
            DayCardCountEntry(DayCardStatusEnum.APPROVED, 0L, 0),
            DayCardCountEntry(DayCardStatusEnum.DONE, 0L, 0),
            DayCardCountEntry(DayCardStatusEnum.NOTDONE, 10L, 0),
            DayCardCountEntry(DayCardStatusEnum.OPEN, 10L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(0L)
  }

  @Test
  fun calculatePpc_bigNumbersZeroPercent() {
    val list =
        listOf(
            DayCardCountEntry(DayCardStatusEnum.APPROVED, 1L, 0),
            DayCardCountEntry(DayCardStatusEnum.NOTDONE, 10000L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(0L)
  }

  @Test
  fun calculatePpc_floorResult() {
    val list =
        listOf(
            DayCardCountEntry(DayCardStatusEnum.APPROVED, 100000L, 0),
            DayCardCountEntry(DayCardStatusEnum.NOTDONE, 1L, 0))

    assertThat(PpcCalculator(list).total()).isEqualTo(99L)
  }
}
