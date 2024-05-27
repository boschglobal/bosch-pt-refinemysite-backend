/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum

abstract class StatusCountEntry(val status: DayCardStatusEnum, val count: Long, week: Long) :
    WeekEntry(week)
