/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.model.dto

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import java.time.LocalDate

data class TaskScheduleDto(
    // Schedule information
    val start: LocalDate? = null,
    val end: LocalDate? = null,

    // Task information
    val taskIdentifier: TaskId
)
