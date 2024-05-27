/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary

import java.time.LocalDate

object ExportTimeRangeValidator {

  private const val CALENDAR_EXPORT_LIMIT_YEARS = 3

  fun validate(from: LocalDate, to: LocalDate) =
      require(
          from.isBefore(to) && from.plusYears(CALENDAR_EXPORT_LIMIT_YEARS.toLong()).isAfter(to)) {
        "The requested timespan is not valid."
      }
}
