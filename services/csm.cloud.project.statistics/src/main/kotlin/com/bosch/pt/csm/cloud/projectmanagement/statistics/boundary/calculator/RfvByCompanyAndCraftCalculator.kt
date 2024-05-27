/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.TimeFrame
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountGroupedEntry
import java.time.LocalDate

/**
 * Calculates the Reason for Variance (RFV) statistic for day cards grouped by company and craft.
 *
 * @param dayCardReasonCounts the count of day cards per reason from which the RFV will be
 * calculated
 */
class RfvByCompanyAndCraftCalculator(
    private val dayCardReasonCounts: List<DayCardReasonCountGroupedEntry>
) : AbstractRfvCalculator() {

  /**
   * Calculates the total RFV of underlying day cards grouped by companies and crafts.
   *
   * @return the total RFV
   */
  fun total() =
      dayCardReasonCounts.groupBy { Pair(it.company, it.craft) }.mapValues {
        calculateRfv(it.value)
      }

  /**
   * Calculates the RFV per week.
   *
   * @param rootDate the start date that was used to calculate the list of the day card reason
   * counts; the start date dictates the first day of a week
   * @return the list of RFV values
   */
  fun perWeek(rootDate: LocalDate) =
      dayCardReasonCounts
          .groupBy { Triple(it.company, it.craft, TimeFrame(it.week, rootDate)) }
          .mapValues { calculateRfv(it.value) }
}
