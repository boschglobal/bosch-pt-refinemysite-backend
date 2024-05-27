/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.model

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId

class DayCardStatusCountProjection(
    val taskIdentifier: TaskId,
    val status: DayCardStatusEnum,
    val count: Long
)
