/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.TimeFrame
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountEntry
import java.time.LocalDate

/**
 * Calculates the Reason for Variance (RFV) statistic for day cards.
 *
 * @param dayCardReasonCounts the count of day cards per reason from which the RFV will be
 * calculated
 */
class RfvCalculator(private val dayCardReasonCounts: List<DayCardReasonCountEntry>) :
    AbstractRfvCalculator() {

  /**
   * Calculates the total RFV of underlying dayCards.
   *
   * @return the total RFV
   */
  fun total() = calculateRfv(dayCardReasonCounts)

  /**
   * Calculates the RFV per week.
   *
   * @param rootDate the start date that was used to calculate the list of the dayCardReasonCounts;
   * the start date dictates the first day of a week
   * @return the map of RFV values
   */
  fun perWeek(rootDate: LocalDate) =
      dayCardReasonCounts.groupBy { TimeFrame(it.week, rootDate) }.mapValues {
        calculateRfv(it.value)
      }
}
