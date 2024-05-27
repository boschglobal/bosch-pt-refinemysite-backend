/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.ReasonCountEntry

open class AbstractRfvCalculator {

  fun calculateRfv(dayCardReasonCounts: List<ReasonCountEntry>) =
      dayCardReasonCounts.groupBy { it.reason }.mapValues {
        it.value.sumOf { reasonCount -> reasonCount.count }
      }
}
