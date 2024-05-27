/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum

abstract class ReasonCountEntry(
    val reason: DayCardReasonVarianceEnum,
    val count: Long,
    week: Long
) : WeekEntry(week)
