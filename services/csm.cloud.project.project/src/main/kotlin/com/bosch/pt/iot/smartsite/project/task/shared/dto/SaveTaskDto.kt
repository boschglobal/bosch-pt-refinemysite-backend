/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.dto

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId

open class SaveTaskDto(
    val name: String,
    val description: String?,
    val location: String?,
    val status: TaskStatusEnum,
    val projectCraftIdentifier: ProjectCraftId,
    val assigneeIdentifier: ParticipantId?,
    val workAreaIdentifier: WorkAreaId?
)
