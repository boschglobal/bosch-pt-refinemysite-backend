/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import java.time.LocalDate

data class TimeFrame(val week: Long, val rootDate: LocalDate) {

  val startDate: LocalDate = rootDate.plusWeeks(week)
  val endDate: LocalDate = rootDate.plusWeeks(week + 1L).minusDays(1L)
}
