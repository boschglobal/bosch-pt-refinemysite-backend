/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header

data class WeekDayCell(val name: String, val type: WeekDayCellType)

enum class WeekDayCellType(val value: String) {
  WORKING_DAY("working-day"),
  RESTING_DAY("resting-day")
}
