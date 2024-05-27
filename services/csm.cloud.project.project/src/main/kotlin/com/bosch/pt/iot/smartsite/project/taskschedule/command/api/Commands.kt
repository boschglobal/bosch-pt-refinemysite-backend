/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.taskschedule.command.api

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.time.LocalDate

data class CreateTaskScheduleCommand(
    val identifier: TaskScheduleId,
    val taskIdentifier: TaskId,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val slots: Map<DayCardId, LocalDate>? = null
)

data class UpdateTaskScheduleCommand(
    val identifier: TaskScheduleId,
    val taskIdentifier: TaskId,
    val version: Long?,
    val start: LocalDate?,
    val end: LocalDate?,
    val slots: Map<DayCardId, LocalDate>?
)

data class UpdateTaskScheduleSlotsForImportCommand(
    val taskIdentifier: TaskId,
    val slots: Map<DayCardId, LocalDate>
)

data class AddDayCardsToTaskScheduleCommand(
    val taskScheduleIdentifier: TaskScheduleId,
    val projectIdentifier: ProjectId,
    val taskIdentifier: TaskId,
    val date: LocalDate,
    val dayCardIdentifier: DayCardId,
    val eTag: ETag
)

data class RemoveDayCardsFromTaskScheduleCommand(
    val identifier: DayCardId,
    val taskScheduleIdentifier: TaskScheduleId,
    val scheduleETag: ETag
)

data class DeleteTaskScheduleCommand(val taskIdentifier: TaskId, val eTag: ETag)
