/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.TimeFrame
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountEntry
import java.time.LocalDate

/**
 * Calculates the Percent Plan Complete (PPC) statistic for day cards.
 *
 * * @param dayCardCounts the count of day cards per day card status from which the PPC will be
 * calculated
 */
class PpcCalculator(private val dayCardCounts: List<DayCardCountEntry>) : AbstractPpcCalculator() {

  /**
   * Calculates the total PPC of underlying day cards.
   *
   * @return the total PPC
   */
  fun total() = calculatePpc(dayCardCounts)

  /**
   * Calculates the PPC per week.
   *
   * @param rootDate the start date that was used to calculate the list of the dayCardCounts; the
   * start date dictates the first day of a week
   * @return the list of PPC values
   */
  fun perWeek(rootDate: LocalDate): Map<TimeFrame, Long?> =
      dayCardCounts.groupBy { TimeFrame(it.week, rootDate) }.mapValues { calculatePpc(it.value) }
}
