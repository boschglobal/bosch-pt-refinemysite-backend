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
import java.util.UUID

class DayCardCountGroupedEntry(
    status: DayCardStatusEnum,
    count: Long,
    week: Long,
    val company: UUID?,
    val craft: UUID
) : StatusCountEntry(status, count, week) {

  // Used by the repository
  constructor(
      status: DayCardStatusEnum,
      count: Long,
      week: Int,
      company: UUID?,
      craft: UUID
  ) : this(status, count, week.toLong(), company, craft)
}

fun List<DayCardCountGroupedEntry>.toRfv() =
    filter { it.status == DayCardStatusEnum.OPEN }
        .map {
          // an OPEN day card also counts as a reason for variance (RFV)
          DayCardReasonCountGroupedEntry(
              DayCardReasonVarianceEnum.OPEN, it.count, it.week, it.company, it.craft)
        }
