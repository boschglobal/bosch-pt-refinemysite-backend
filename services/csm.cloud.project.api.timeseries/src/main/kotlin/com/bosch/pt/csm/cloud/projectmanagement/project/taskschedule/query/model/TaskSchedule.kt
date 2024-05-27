/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val TASK_SCHEDULE_PROJECTION = "TaskScheduleProjection"

@Document(TASK_SCHEDULE_PROJECTION)
@TypeAlias(TASK_SCHEDULE_PROJECTION)
data class TaskSchedule(
    @Id val identifier: TaskScheduleId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val taskVersion: Long,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val slots: List<TaskScheduleSlot>,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<TaskScheduleVersion>
)

data class TaskScheduleSlot(
    val date: LocalDate,
    val dayCardId: DayCardId,
    val dayCardVersion: Long
)

data class TaskScheduleVersion(
    val version: Long,
    val taskVersion: Long,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val slots: List<TaskScheduleSlot>,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
