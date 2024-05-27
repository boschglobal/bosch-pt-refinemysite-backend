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

class DayCardReasonCountEntry(reason: DayCardReasonVarianceEnum, count: Long, week: Long) :
    ReasonCountEntry(reason, count, week) {

  // Used by the repository
  constructor(
      reason: DayCardReasonNotDoneEnum,
      count: Long,
      week: Int,
  ) : this(DayCardReasonVarianceEnum.valueOf(reason.name), count, week.toLong())
}
