/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.MonthRow
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekRow
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarRow
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.legend.LegendRow

data class Calendar(
    val projectName: String,
    val exportDate: String,
    val calendarEmptyMessage: String,
    val legendRow: LegendRow,
    val monthRow: MonthRow,
    val weekRow: WeekRow,
    val rows: List<CalendarRow>,
    val includeDayCards: Boolean,
    val includeMilestones: Boolean
) {
  val expandedSmall = !includeDayCards && includeMilestones
  val expandedLarge = includeDayCards
  val expanded: Boolean = includeDayCards || includeMilestones
}
