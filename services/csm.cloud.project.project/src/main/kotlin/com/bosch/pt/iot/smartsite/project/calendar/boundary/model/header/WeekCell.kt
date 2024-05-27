/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header

data class WeekCell(
    val name: String,
    val startDate: String,
    val endDate: String,
    val days: List<WeekDayCell>
)
