/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCellType.BLANK
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCellType.DAYCARD
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCellType.OUT_OF_SCHEDULE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.DONE
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import java.util.stream.Collectors
import org.springframework.stereotype.Component

@Component
class DayCardCellAssembler {

  fun assemble(
      schedule: TaskScheduleWithDayCardsDto,
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      workdayConfiguration: WorkdayConfiguration
  ): List<DayCardCell> {
    val start = schedule.start!!
    val end = schedule.end!!
    val startOfWeek = workdayConfiguration.startOfWeek

    val dateToSlotWithDayCard: Map<LocalDate, TaskScheduleSlotWithDayCardDto> =
        schedule.scheduleSlotsWithDayCards.associateBy { it.slotsDate }

    /*
     * This code calculate the "start of the week of the task visible in the calendar" and
     * the "end of the week of the task visible in the calendar" ( plus one day not included )
     * generating all the days between as elements that will be processed in the map function.
     *
     * The "start of the week of the task visible in the calendar" is the max date between
     * the "start of the week of the calendar" and the "start of the week of the task".
     *
     * The "end of the week of the task visible in the calendar" is the min date between
     * the "end of the week of the calendar" and the "end of the week of the task".
     *
     * For each day element, a day card data will be created with the following type:
     * 1. DAYCARD, represent an existing day card for that day.
     * 2. BLANK, represent a no-existing day card for that day.
     * 3. OUT_OF_SCHEDULE, represent a day that is out of scope from the start and end
     *    of the task but inside the task data display week.
     *
     * Refer to Miro Calendar PDF Export for a visual examples.
     */
    return CalendarDateHelper.getStartOfWeekOfTaskVisibleInCalendar(
            calendarStart, start, startOfWeek)
        .datesUntil(
            CalendarDateHelper.getEndOfWeekOfTaskVisibleInCalendar(calendarEnd, end, startOfWeek)
                .plusDays(1))
        .map { dayOfWeek ->

          /*
           * Check if the week day if out of the task start and end dates and generate an OUT_OF_SCHEDULE DayCardCell
           * If not create a DAYCARD or BLANK DayCardCell based on the existing of a day card.
           */
          if (dayOfWeek.isBefore(start) || dayOfWeek.isAfter(end)) {
            assembleOutOfSchedule()
          } else {
            dateToSlotWithDayCard[dayOfWeek]?.run { assembleDayCard(this) } ?: assembleBlank()
          }
        }
        .collect(Collectors.toList())
  }

  private fun assembleDayCard(slotWithDayCard: TaskScheduleSlotWithDayCardDto) =
      DayCardCell(
          slotWithDayCard.slotsDayCardTitle,
          slotWithDayCard.slotsDayCardManpower.stripTrailingZeros().toPlainString(),
          slotWithDayCard.slotsDayCardStatus,
          DAYCARD)

  private fun assembleBlank() = DayCardCell("", "", DONE, BLANK)

  private fun assembleOutOfSchedule() = DayCardCell("", "", DONE, OUT_OF_SCHEDULE)
}
