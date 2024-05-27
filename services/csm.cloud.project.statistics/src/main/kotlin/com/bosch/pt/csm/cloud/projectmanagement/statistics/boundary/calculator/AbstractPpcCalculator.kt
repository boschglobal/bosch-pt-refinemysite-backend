/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum.DONE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.StatusCountEntry

open class AbstractPpcCalculator {

  fun calculatePpc(statusCounts: List<StatusCountEntry>): Long? {
    val totalCount = statusCounts.sumOf { it.count }

    if (totalCount <= 0) {
      return null
    }
    val approvedOrDoneCount =
        statusCounts.filter { it.status == APPROVED || it.status == DONE }.sumOf { it.count }

    // integer division by purpose to truncate decimals
    return 100 * approvedOrDoneCount / totalCount
  }
}
