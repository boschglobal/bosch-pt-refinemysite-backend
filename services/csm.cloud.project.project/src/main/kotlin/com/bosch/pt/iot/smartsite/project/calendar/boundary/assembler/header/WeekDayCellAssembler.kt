/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekDayCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekDayCellType.RESTING_DAY
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekDayCellType.WORKING_DAY
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import org.springframework.stereotype.Component

@Component
class WeekDayCellAssembler {

  /*
   * This code uses the "first day of the week", calculate the "last day of the week"
   * ( plus one day not included ) and generates one date for each day between the two values.
   *
   * Each date will be used to build one WeekDayCell with the property "day type"
   * being a working day or a rest day.
   */
  fun assemble(
      firstDayOfWeek: LocalDate,
      workingDays: Set<DayOfWeek>,
      holidays: Set<LocalDate>,
      weekDayFormatter: DateTimeFormatter
  ): List<WeekDayCell> =
      firstDayOfWeek
          .datesUntil(firstDayOfWeek.plusDays(WEEK_LENGTH))
          .map { dayOfWeek ->
            WeekDayCell(
                dayOfWeek.format(weekDayFormatter),
                determineDayType(dayOfWeek, workingDays, holidays))
          }
          .collect(Collectors.toList())

  private fun determineDayType(
      day: LocalDate,
      workingDays: Set<DayOfWeek>,
      holidays: Set<LocalDate>
  ) =
      if (workingDays.contains(day.dayOfWeek) && !holidays.contains(day)) WORKING_DAY
      else RESTING_DAY

  companion object {
    // The days of the week that need to be mark for a different style
    private const val WEEK_LENGTH = 7L
  }
}
