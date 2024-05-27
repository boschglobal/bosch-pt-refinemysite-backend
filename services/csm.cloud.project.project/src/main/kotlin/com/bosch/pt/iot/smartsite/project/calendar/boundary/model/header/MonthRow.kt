/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header

data class MonthRow(val year: Int, val months: List<MonthCell>) {

  val length = months.sumOf(MonthCell::length)
}
