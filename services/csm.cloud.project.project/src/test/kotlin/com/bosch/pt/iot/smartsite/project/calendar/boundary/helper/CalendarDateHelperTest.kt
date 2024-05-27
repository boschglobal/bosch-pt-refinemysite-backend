/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.helper

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveFirstDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveLastDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveWeekNumber
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CalendarDateHelperTest {

  @Test
  fun `verify calculation of week number for start of week equal to monday in 2022`() {
    val lastWeekOfYear = LocalDate.of(2022, 1, 2)
    val firstWeekOfNewYear = LocalDate.of(2022, 1, 3)
    val secondWeekOfNewYear = LocalDate.of(2022, 1, 10)

    assertThat(lastWeekOfYear.retrieveWeekNumber(MONDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(MONDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(MONDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to monday in 2023`() {
    val lastWeekOfYear = LocalDate.of(2023, 1, 1)
    val firstWeekOfNewYear = LocalDate.of(2023, 1, 2)
    val secondWeekOfNewYear = LocalDate.of(2023, 1, 9)

    assertThat(lastWeekOfYear.retrieveWeekNumber(MONDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(MONDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(MONDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to wednesday in 2022`() {
    val lastWeekOfYear = LocalDate.of(2022, 1, 4)
    val firstWeekOfNewYear = LocalDate.of(2023, 1, 5)
    val secondWeekOfNewYear = LocalDate.of(2023, 1, 12)

    assertThat(lastWeekOfYear.retrieveWeekNumber(WEDNESDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(WEDNESDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(WEDNESDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to wednesday in 2023`() {
    val lastWeekOfYear = LocalDate.of(2023, 1, 3)
    val firstWeekOfNewYear = LocalDate.of(2023, 1, 4)
    val secondWeekOfNewYear = LocalDate.of(2023, 1, 11)

    assertThat(lastWeekOfYear.retrieveWeekNumber(WEDNESDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(WEDNESDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(WEDNESDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to saturday in 2022`() {
    val lastWeekOfYear = LocalDate.of(2021, 12, 31)
    val firstWeekOfNewYear = LocalDate.of(2022, 1, 1)
    val secondWeekOfNewYear = LocalDate.of(2022, 1, 8)

    assertThat(lastWeekOfYear.retrieveWeekNumber(SATURDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(SATURDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(SATURDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to saturday 2023`() {
    val lastWeekOfYear = LocalDate.of(2023, 1, 6)
    val firstWeekOfNewYear = LocalDate.of(2023, 1, 7)
    val secondWeekOfNewYear = LocalDate.of(2023, 1, 14)

    assertThat(lastWeekOfYear.retrieveWeekNumber(SATURDAY)).isEqualTo(53)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(SATURDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(SATURDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to sunday in 2022`() {
    val lastWeekOfYear = LocalDate.of(2022, 1, 1)
    val firstWeekOfNewYear = LocalDate.of(2022, 1, 2)
    val secondWeekOfNewYear = LocalDate.of(2022, 1, 9)

    assertThat(lastWeekOfYear.retrieveWeekNumber(SUNDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(SUNDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(SUNDAY)).isEqualTo(2)
  }

  @Test
  fun `verify calculation of week number for start of week equal to sunday 2023`() {
    val lastWeekOfYear = LocalDate.of(2022, 12, 31)
    val firstWeekOfNewYear = LocalDate.of(2023, 1, 1)
    val secondWeekOfNewYear = LocalDate.of(2023, 1, 8)

    assertThat(lastWeekOfYear.retrieveWeekNumber(SUNDAY)).isEqualTo(52)
    assertThat(firstWeekOfNewYear.retrieveWeekNumber(SUNDAY)).isEqualTo(1)
    assertThat(secondWeekOfNewYear.retrieveWeekNumber(SUNDAY)).isEqualTo(2)
  }

  // Case 1: Export Start Week Day < start day of the week
  @Test
  fun `verify retrieve of the first day of the week for export start week day before start day of the week`() {
    val exportStart = LocalDate.of(2019, 10, 7)
    val firstDayOfWeekVisibleInCalendar = LocalDate.of(2019, 10, 1)

    assertThat(exportStart.retrieveFirstDayOfWeek(TUESDAY))
        .isEqualTo(firstDayOfWeekVisibleInCalendar)
  }

  // Case 2: Export Start Week Day > start day of the week
  @Test
  fun `verify retrieve of the first day of the week for export start week day after start day of the week`() {
    val exportStart = LocalDate.of(2019, 10, 9)
    val firstDayOfWeekVisibleInCalendar = LocalDate.of(2019, 10, 8)

    assertThat(exportStart.retrieveFirstDayOfWeek(TUESDAY))
        .isEqualTo(firstDayOfWeekVisibleInCalendar)
  }

  // Case 3: Export Start Week Day = start day of the week
  @Test
  fun `verify retrieve of the first day of the week for export start week day equal start day of the week`() {
    val exportStart = LocalDate.of(2019, 10, 8)
    val firstDayOfWeekVisibleInCalendar = LocalDate.of(2019, 10, 8)

    assertThat(exportStart.retrieveFirstDayOfWeek(TUESDAY))
        .isEqualTo(firstDayOfWeekVisibleInCalendar)
  }

  // Case 4: Export End Week Day < start day of the week
  @Test
  fun `verify retrieve of the last day of the week for export end week day before start day of the week`() {
    val exportEnd = LocalDate.of(2019, 10, 7)
    val lastDayOfWeekVisibleInCalendar = LocalDate.of(2019, 10, 7)

    assertThat(exportEnd.retrieveLastDayOfWeek(TUESDAY)).isEqualTo(lastDayOfWeekVisibleInCalendar)
  }

  // Case 5: Export End Week Day > start day of the week
  @Test
  fun `verify retrieve of the last day of the week for export end week day after start day of the week`() {
    val exportEnd = LocalDate.of(2019, 10, 9)
    val lastDayOfWeekVisibleInCalendar = LocalDate.of(2019, 10, 14)

    assertThat(exportEnd.retrieveLastDayOfWeek(TUESDAY)).isEqualTo(lastDayOfWeekVisibleInCalendar)
  }

  // Case 6: Export End Week Day = start day of the week
  @Test
  fun `verify retrieve of the last day of the week for export end week day equal start day of the week`() {
    val exportEnd = LocalDate.of(2019, 10, 8)
    val lastDayOfWeekVisibleInCalendar = LocalDate.of(2019, 10, 14)

    assertThat(exportEnd.retrieveLastDayOfWeek(TUESDAY)).isEqualTo(lastDayOfWeekVisibleInCalendar)
  }
}
