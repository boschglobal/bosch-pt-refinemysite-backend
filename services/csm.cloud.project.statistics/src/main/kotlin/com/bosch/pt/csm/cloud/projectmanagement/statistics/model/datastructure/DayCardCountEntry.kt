/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum

class DayCardCountEntry(status: DayCardStatusEnum, count: Long, week: Double) :
    StatusCountEntry(status, count, week.toLong()) {

  // Used by the repository
  constructor(
      status: DayCardStatusEnum,
      count: Long,
      week: Int
  ) : this(status, count, week.toDouble())
}

fun List<DayCardCountEntry>.toRfv() =
    filter { it.status == DayCardStatusEnum.OPEN }.map {
      // an OPEN day card also counts as a reason for variance (RFV)
      DayCardReasonCountEntry(DayCardReasonVarianceEnum.OPEN, it.count, it.week)
    }
