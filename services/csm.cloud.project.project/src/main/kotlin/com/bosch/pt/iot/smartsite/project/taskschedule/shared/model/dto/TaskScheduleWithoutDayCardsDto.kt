/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.time.LocalDate
import java.util.Date

class TaskScheduleWithoutDayCardsDto(
    // Schedule information
    val identifier: TaskScheduleId,
    val version: Long,
    val start: LocalDate?,
    val end: LocalDate?,
    // Schedule Create User information
    val createdByIdentifier: UserId?,
    val createdDate: Date?,
    // Schedule Modify User information
    val lastModifiedByIdentifier: UserId?,
    val lastModifiedDate: Date?,
    // Task information
    val taskIdentifier: TaskId,
    val taskName: String,
    // Project information
    val taskProjectIdentifier: ProjectId
)
