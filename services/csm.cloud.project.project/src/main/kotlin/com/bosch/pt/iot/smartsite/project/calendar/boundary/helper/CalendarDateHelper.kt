/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.helper

import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

object CalendarDateHelper {

  private const val DAYS_OF_WEEK = 7
  private const val MILESTONE_WIDTH_DAYS = 4
  private const val MILESTONE_SHIFT_DAYS = 3L

  fun getStartOfWeekOfTaskVisibleInCalendar(
      calendarStart: LocalDate,
      taskStart: LocalDate,
      startOfWeek: DayOfWeek = MONDAY
  ) =
      maxOf(
          calendarStart.retrieveFirstDayOfWeek(startOfWeek),
          taskStart.retrieveFirstDayOfWeek(startOfWeek))

  fun getEndOfWeekOfTaskVisibleInCalendar(
      calendarEnd: LocalDate,
      taskEnd: LocalDate,
      startOfWeek: DayOfWeek = MONDAY
  ) =
      minOf(
          calendarEnd.retrieveLastDayOfWeek(startOfWeek),
          taskEnd.retrieveLastDayOfWeek(startOfWeek))

  /*
   * This function calculate the "start date of the milestone visible in the calendar"
   * comparing the date of the milestone against the "last day visible in the calendar".
   *
   * If the difference is more than the milestone width the date is adjusted.
   */
  fun getStartOfMilestoneVisibleInCalendar(
      lastDayVisibleInCalendar: LocalDate,
      milestoneDate: LocalDate
  ): LocalDate =
      if (milestoneDate.until(lastDayVisibleInCalendar.plusDays(1), ChronoUnit.DAYS) <
          MILESTONE_WIDTH_DAYS)
          milestoneDate.minusDays(MILESTONE_SHIFT_DAYS)
      else milestoneDate

  // This function calculate the number of the week based on the start of the week.
  fun LocalDate.retrieveWeekNumber(startOfWeek: DayOfWeek = MONDAY) =
      this[WeekFields.of(startOfWeek, DAYS_OF_WEEK).weekOfWeekBasedYear()]

  /*
   * This function calculates the difference in days between the date and the star of the week.
   * If the value is negative the "first day of the week" will be in the previous week of the date,
   * otherwise it will be in the same week.
   *
   * Refer to Miro Calendar PDF Export for a visual examples.
   */
  fun LocalDate.retrieveFirstDayOfWeek(startOfWeek: DayOfWeek = MONDAY): LocalDate {
    val differenceBetweenDaysOfWeek = (this.dayOfWeek.value - startOfWeek.value).toLong()

    return if (differenceBetweenDaysOfWeek < 0) {
      this.minusDays((DAYS_OF_WEEK + differenceBetweenDaysOfWeek))
    } else {
      this.minusDays(differenceBetweenDaysOfWeek)
    }
  }

  /*
   * This function calculates the difference in days between the date and the star of the week.
   * If the value is positive the "last day of the week" will be in the same week of the date,
   * otherwise it will be in the next week.
   *
   * Note: the "last day of the week" will always be one day previous to the next "start day of the week"
   *
   * Refer to Miro Calendar PDF Export for a visual examples.
   */
  fun LocalDate.retrieveLastDayOfWeek(startOfWeek: DayOfWeek = MONDAY): LocalDate {
    val differenceBetweenDaysOfWeek = (this.dayOfWeek.value - startOfWeek.value).toLong()

    return if (differenceBetweenDaysOfWeek >= 0) {
      this.plusDays(DAYS_OF_WEEK - differenceBetweenDaysOfWeek).minusDays(1)
    } else {
      this.minusDays(differenceBetweenDaysOfWeek).minusDays(1)
    }
  }
}
