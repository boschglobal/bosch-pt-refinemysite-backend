/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonNotDoneEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import java.util.UUID

class DayCardReasonCountGroupedEntry(
    reason: DayCardReasonVarianceEnum,
    count: Long,
    week: Long,
    val company: UUID?,
    val craft: UUID
) : ReasonCountEntry(reason, count, week) {

  // Used by the repository
  @Suppress("unused")
  constructor(
      reason: DayCardReasonNotDoneEnum,
      count: Long,
      week: Int,
      company: UUID?,
      craft: UUID
  ) : this(DayCardReasonVarianceEnum.valueOf(reason.name), count, week.toLong(), company, craft)
}
