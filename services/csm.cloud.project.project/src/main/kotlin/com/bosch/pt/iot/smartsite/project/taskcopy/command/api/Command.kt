/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.command.api

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskcopy.shared.dto.OverridableTaskParametersDto
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId

data class TaskCopyCommand(
    val copyFromIdentifier: TaskId,
    val identifier: TaskId = TaskId(),
    val shiftDays: Long,
    val includeDayCards: Boolean,
    val parametersOverride: OverridableTaskParametersDto? = null
)

data class TaskScheduleCopyCommand(
    val copyFromIdentifier: TaskScheduleId,
    val copyToTaskIdentifier: TaskId,
    val identifier: TaskScheduleId = TaskScheduleId(),
    val shiftDays: Long,
    val includeDayCards: Boolean,
)

data class DayCardCopyCommand(
    val copyFromIdentifier: DayCardId,
    val identifier: DayCardId = DayCardId(),
    val copyToTaskScheduleIdentifier: TaskScheduleId,
    val shiftDays: Long
)
