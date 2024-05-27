/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import java.util.UUID

data class TaskConstraintSelectionDto(
    val identifier: UUID,
    val version: Long,
    val taskIdentifier: TaskId,
    val constraints: List<TaskConstraintEnum>
)
