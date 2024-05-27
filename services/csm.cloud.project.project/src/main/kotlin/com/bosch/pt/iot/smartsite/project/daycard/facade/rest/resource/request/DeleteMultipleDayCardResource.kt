/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import jakarta.validation.constraints.Size

data class DeleteMultipleDayCardResource(
    @field:Size(min = 1, max = 100) val items: List<DeleteMultipleDayCardFromScheduleResource>
)

data class DeleteMultipleDayCardFromScheduleResource(
    val scheduleVersion: Long,
    val ids: Set<DayCardId>
)
