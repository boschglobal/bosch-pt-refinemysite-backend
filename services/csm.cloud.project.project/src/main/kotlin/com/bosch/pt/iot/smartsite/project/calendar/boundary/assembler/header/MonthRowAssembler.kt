/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.MonthRow
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class MonthRowAssembler(private val monthCellAssembler: MonthCellAssembler) {

  fun assemble(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      workdayConfiguration: WorkdayConfiguration
  ) =
      MonthRow(
          calendarStart.year,
          monthCellAssembler.assemble(calendarStart, calendarEnd, workdayConfiguration))
}
