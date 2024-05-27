/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveFirstDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveLastDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveWeekNumber
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarMessageTranslationHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekCell
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors.toList
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class WeekCellAssembler(
    private val weekDayCellAssembler: WeekDayCellAssembler,
    private val calendarMessageTranslationHelper: CalendarMessageTranslationHelper
) {

  fun assemble(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean,
      includeMilestones: Boolean
  ): List<WeekCell> {

    val startOfWeek = workdayConfiguration.startOfWeek
    val weekName = calendarMessageTranslationHelper.getWeekCellName()

    val weekPattern = calendarMessageTranslationHelper.getWeekCellPattern()
    val weekFormatter = DateTimeFormatter.ofPattern(weekPattern, LocaleContextHolder.getLocale())

    val weekDayPattern =
        calendarMessageTranslationHelper.getWeekDayCellPattern(includeDayCards, includeMilestones)
    val weekDayFormatter =
        DateTimeFormatter.ofPattern(weekDayPattern, LocaleContextHolder.getLocale())

    val workdays = workdayConfiguration.workingDays
    val holidays = workdayConfiguration.holidays.map { it.date }.toSet()

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
     * Each date will be used to calculate the week days,as well as the first and last day of the week.
     */
    return calendarStart
        .retrieveFirstDayOfWeek(startOfWeek)
        .datesUntil(calendarEnd.retrieveLastDayOfWeek(startOfWeek).plusDays(1), Period.ofWeeks(1))
        .map { firstDayOfWeek ->
          val days =
              if (includeDayCards || includeMilestones)
                  weekDayCellAssembler.assemble(
                      firstDayOfWeek, workdays, holidays, weekDayFormatter)
              else emptyList()

          WeekCell(
              "$weekName ${firstDayOfWeek.retrieveWeekNumber(startOfWeek)}",
              firstDayOfWeek.format(weekFormatter),
              firstDayOfWeek.plusDays(LENGTH_TO_LAST_DAY_OF_WEEK).format(weekFormatter),
              days)
        }
        .collect(toList())
  }

  companion object {
    private const val LENGTH_TO_LAST_DAY_OF_WEEK = 6L
  }
}
