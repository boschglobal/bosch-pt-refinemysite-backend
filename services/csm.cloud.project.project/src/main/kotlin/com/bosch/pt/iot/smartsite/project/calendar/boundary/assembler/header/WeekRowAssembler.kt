/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarMessageTranslationHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekRow
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class WeekRowAssembler(
    private val weekCellAssembler: WeekCellAssembler,
    private val calendarMessageTranslationHelper: CalendarMessageTranslationHelper
) {

  fun assemble(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean,
      includeMilestones: Boolean
  ) =
      WeekRow(
          calendarMessageTranslationHelper.getWeekRowName(),
          calendarStart.year,
          weekCellAssembler.assemble(
              calendarStart, calendarEnd, workdayConfiguration, includeDayCards, includeMilestones))
}
