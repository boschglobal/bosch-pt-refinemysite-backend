/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveFirstDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveLastDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.MonthCell
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit.DAYS
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class MonthCellAssembler {

  fun assemble(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      workdayConfiguration: WorkdayConfiguration
  ): List<MonthCell> {
    val locale = LocaleContextHolder.getLocale()
    val startOfWeek = workdayConfiguration.startOfWeek
    val lengthPerMonth = mutableMapOf<YearMonth, Long>()

    /*
     * This code calculate the "first day visible in the calendar" and the "last day visible
     * in the calendar" ( plus one day not included ) and generates one date for each week
     * between the two values.
     *
     * Each date element will correspond to the first day of each week and the day of week
     * used be the same as the "first day visible in the calendar".
     *
     * For a "first day visible in the calendar" date equal to Tuesday 2019-10-01 the generated
     * dates will be the following example:
     * - First date will be Tuesday 2019-10-01
     * - Second date will be Tuesday 2019-10-08
     * - Third date will be Tuesday 2019-10-15
     *
     * Each date will be used to calculate the with of the corresponding month in
     * an iterative process ( multiple elements (weeks) can correspond to one month ).
     */
    calendarStart
        .retrieveFirstDayOfWeek(startOfWeek)
        .datesUntil(calendarEnd.retrieveLastDayOfWeek(startOfWeek).plusDays(1), Period.ofWeeks(1))
        .forEach { firstDayOfWeek ->
          val currentMonth = YearMonth.from(firstDayOfWeek)
          val daysUntilEndOfMonth =
              DAYS.between(firstDayOfWeek, currentMonth.plusMonths(1).atDay(1))
          val weekInSingleMonth = daysUntilEndOfMonth >= WEEK_LENGTH

          /*
           * Verify if the week is inside the specific month or if it is split between two months.
           * In the second case calculates the specific length for each month.
           */
          if (weekInSingleMonth) {
            lengthPerMonth
                .getOrElse(currentMonth) { 0L }
                .run { lengthPerMonth[currentMonth] = this + COLUMN_WIDTH }
          } else {
            val nextMonth = YearMonth.from(firstDayOfWeek).plusMonths(1)
            val widthNextMonth = COLUMN_WIDTH - daysUntilEndOfMonth

            lengthPerMonth
                .getOrElse(currentMonth) { 0L }
                .run { lengthPerMonth[currentMonth] = this + daysUntilEndOfMonth }
            lengthPerMonth
                .getOrElse(nextMonth) { 0L }
                .run { lengthPerMonth[nextMonth] = this + widthNextMonth }
          }
        }

    return lengthPerMonth.map {
      MonthCell(it.key.month.getDisplayName(TextStyle.FULL, locale), it.value)
    }
  }

  companion object {
    /*
     * The width of a column, i.e. the number of table cells that are merged horizontally (think of
     * HTML's colspan attribute) to form a single columns
     */
    private const val COLUMN_WIDTH = 7
    private const val WEEK_LENGTH = 7
  }
}
