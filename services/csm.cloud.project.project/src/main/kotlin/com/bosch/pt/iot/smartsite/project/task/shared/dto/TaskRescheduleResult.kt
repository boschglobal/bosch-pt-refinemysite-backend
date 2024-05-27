/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.dto

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId

data class TaskRescheduleResult(
    /**
     * task identifiers that can be rescheduled successfully (or have already been rescheduled
     * successfully)
     */
    val successful: List<TaskId> = emptyList(),

    /**
     * task identifiers that cannot be rescheduled successfully (or have already been rescheduled
     * unsuccessfully)
     */
    val failed: List<TaskId> = emptyList(),
)
