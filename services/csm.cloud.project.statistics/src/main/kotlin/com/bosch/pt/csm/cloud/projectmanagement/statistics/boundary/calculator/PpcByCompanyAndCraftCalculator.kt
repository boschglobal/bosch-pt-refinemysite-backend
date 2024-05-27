/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.TimeFrame
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountGroupedEntry
import java.time.LocalDate

/**
 * Calculates the Percent Plan Complete (PPC) statistic for day cards, per company and craft.
 *
 * @param dayCardCounts the count of day cards grouped by company, craft, day card status (in this
 * order), from which the PPC values will be calculated.
 */
class PpcByCompanyAndCraftCalculator(private val dayCardCounts: List<DayCardCountGroupedEntry>) :
    AbstractPpcCalculator() {

  /**
   * Calculates the total PPC of underlying day cards, grouped by company and craft.
   *
   * @return the total PPC per group; the key holds a <company, craft> identifier pair, the value is
   * the corresponding calculation result.
   */
  fun total() =
      dayCardCounts.groupBy { Pair(it.company, it.craft) }.mapValues { calculatePpc(it.value) }

  /**
   * Calculates the Percent Plan Complete (PPC) statistic for company and craft per week.
   *
   * @param rootDate the start date that was used to calculate the list of the dayCardCounts; the
   * start date dictates the first day of a week
   * @return the total PPC per group; the key holds a <company, craft, timeframe> triple, the value
   * is the corresponding calculated result. The 'timespan' consists of the root date and the week
   * offset.
   */
  fun perWeek(rootDate: LocalDate) =
      dayCardCounts
          .groupBy { Triple(it.company, it.craft, TimeFrame(it.week, rootDate)) }
          .mapValues { calculatePpc(it.value) }
}
