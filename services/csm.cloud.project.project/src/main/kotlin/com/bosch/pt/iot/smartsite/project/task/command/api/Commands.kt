/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.task.command.api

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId

data class CreateTaskCommand(
    val identifier: TaskId,
    val projectIdentifier: ProjectId,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val projectCraftIdentifier: ProjectCraftId,
    val assigneeIdentifier: ParticipantId? = null,
    val workAreaIdentifier: WorkAreaId? = null,
    val status: TaskStatusEnum
)

data class UpdateTaskCommand(
    val identifier: TaskId,
    val projectIdentifier: ProjectId,
    val version: Long?,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val projectCraftIdentifier: ProjectCraftId,
    val workAreaIdentifier: WorkAreaId? = null,
    val assigneeIdentifier: ParticipantId? = null,
    val status: TaskStatusEnum
)

data class UpdateTaskStatusCommand(val identifier: TaskId, val status: TaskStatusEnum)

data class UpdateTaskAssignmentCommand(
    val identifier: TaskId,
    val assigneeIdentifier: ParticipantId
)

data class DeleteTaskCommand(val identifier: TaskId)
