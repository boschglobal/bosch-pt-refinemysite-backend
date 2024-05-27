/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import java.time.LocalDate
import java.time.LocalDateTime

data class TaskResource(
    val id: TaskId,
    val version: Long,
    val project: ProjectId,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val craft: ProjectCraftId,
    val assignee: ParticipantId? = null,
    val status: String,
    val editDate: LocalDateTime? = null,
    val workArea: WorkAreaId? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val deleted: Boolean,
    val eventTimestamp: Long
)
