/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScheduleSlotHelperTest {

  @Nested
  inner class `Allow work on non working days` {

    @Test
    fun `verify shifting succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 6)
      val workdayConfiguration =
          buildWorkdayConfiguration(holidays = mutableSetOf(), allowWorkOnNonWorkingDays = true)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(2)
      val slot4 = DayCardId() to dateToFree.plusDays(4)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(1)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(1)
      val expectedSlot3 = slot3.first to slot3.second.plusDays(1)
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }

    @Test
    fun `verify shifting on non working days succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 2)
      val workdayConfiguration =
          buildWorkdayConfiguration(holidays = mutableSetOf(), allowWorkOnNonWorkingDays = true)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(3)
      val slot4 = DayCardId() to dateToFree.plusDays(8)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(1)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(1)
      val expectedSlot3 = slot3.first to slot3.second
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }

    @Test
    fun `verify shifting on holidays succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 2)
      val workdayConfiguration =
          buildWorkdayConfiguration(
              holidays =
                  mutableSetOf(
                      Holiday("holiday_1", dateToFree.plusDays(1)),
                      Holiday("holiday_2", dateToFree.plusDays(8))),
              allowWorkOnNonWorkingDays = true)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(3)
      val slot4 = DayCardId() to dateToFree.plusDays(8)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(1)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(1)
      val expectedSlot3 = slot3.first to slot3.second
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }
  }

  @Nested
  inner class `Not allow work on non working days` {

    @Test
    fun `verify shifting succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 6)
      val workdayConfiguration =
          buildWorkdayConfiguration(holidays = mutableSetOf(), allowWorkOnNonWorkingDays = false)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(2)
      val slot4 = DayCardId() to dateToFree.plusDays(4)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(1)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(1)
      val expectedSlot3 = slot3.first to slot3.second.plusDays(1)
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }

    @Test
    fun `verify shifting in non-working days (weekend) succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 2)
      val workdayConfiguration =
          buildWorkdayConfiguration(holidays = mutableSetOf(), allowWorkOnNonWorkingDays = false)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(4)
      val slot4 = DayCardId() to dateToFree.plusDays(8)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(1)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(3)
      val expectedSlot3 = slot3.first to slot3.second.plusDays(1)
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }

    @Test
    fun `verify shifting in non-working days (weekend) with day cards on it succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 3)
      val workdayConfiguration =
          buildWorkdayConfiguration(holidays = mutableSetOf(), allowWorkOnNonWorkingDays = false)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(2)
      val slot3 = DayCardId() to dateToFree.plusDays(3)
      val slot4 = DayCardId() to dateToFree.plusDays(8)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(3)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(2)
      val expectedSlot3 = slot3.first to slot3.second.plusDays(2)
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }

    @Test
    fun `verify shifting in holidays succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 6)
      val workdayConfiguration =
          buildWorkdayConfiguration(
              holidays =
                  mutableSetOf(
                      Holiday("holiday_1", dateToFree.plusDays(2)),
                      Holiday("holiday_2", dateToFree.plusDays(3)),
                      Holiday("holiday_3", dateToFree.plusDays(7))),
              allowWorkOnNonWorkingDays = false)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(7)
      val slot4 = DayCardId() to dateToFree.plusDays(8)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(1)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(3)
      val expectedSlot3 = slot3.first to slot3.second
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }

    @Test
    fun `verify shifting in holidays with day cards on it succeeds`() {
      val dateToFree = LocalDate.of(2023, 3, 6)
      val workdayConfiguration =
          buildWorkdayConfiguration(
              holidays =
                  mutableSetOf(
                      Holiday("holiday_1", dateToFree.plusDays(1)),
                      Holiday("holiday_2", dateToFree.plusDays(7))),
              allowWorkOnNonWorkingDays = false)

      val slot1 = DayCardId() to dateToFree
      val slot2 = DayCardId() to dateToFree.plusDays(1)
      val slot3 = DayCardId() to dateToFree.plusDays(7)
      val slot4 = DayCardId() to dateToFree.plusDays(8)

      val expectedSlot1 = slot1.first to slot1.second.plusDays(2)
      val expectedSlot2 = slot2.first to slot2.second.plusDays(2)
      val expectedSlot3 = slot3.first to slot3.second
      val expectedSlot4 = slot4.first to slot4.second

      val slots = mutableMapOf(slot1, slot2, slot3, slot4)
      val expectedSlots = mapOf(expectedSlot1, expectedSlot2, expectedSlot3, expectedSlot4)

      ScheduleSlotHelper.shiftDayCardOutFromSlot(dateToFree, slots, workdayConfiguration)
      assertThat(slots).doesNotContainValue(dateToFree)
      assertThat(slots).containsExactlyInAnyOrderEntriesOf(expectedSlots)
    }
  }

  private fun buildWorkdayConfiguration(
      holidays: MutableSet<Holiday>,
      allowWorkOnNonWorkingDays: Boolean
  ) =
      WorkdayConfiguration(
          startOfWeek = MONDAY,
          workingDays = mutableSetOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
          holidays = holidays,
          allowWorkOnNonWorkingDays = allowWorkOnNonWorkingDays,
          project = Project())
}
