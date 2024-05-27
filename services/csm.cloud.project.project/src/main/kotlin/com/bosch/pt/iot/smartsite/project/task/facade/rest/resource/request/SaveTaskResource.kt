/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumeration
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.Companion.ENUM_VALUES
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import jakarta.validation.constraints.Size

open class SaveTaskResource(
    val projectId: ProjectId,
    val projectCraftId: ProjectCraftId,
    @field:Size(min = 1, max = Task.MAX_NAME_LENGTH) val name: String,
    @field:Size(max = Task.MAX_DESCRIPTION_LENGTH) val description: String? = null,
    @field:Size(max = Task.MAX_LOCATION_LENGTH) val location: String? = null,
    @field:StringEnumeration(enumClass = TaskStatusEnum::class, enumValues = ENUM_VALUES)
    val status: TaskStatusEnum,
    val assigneeId: ParticipantId? = null,
    val workAreaId: WorkAreaId? = null
)
