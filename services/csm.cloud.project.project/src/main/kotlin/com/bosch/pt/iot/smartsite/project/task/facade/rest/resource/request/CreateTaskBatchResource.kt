/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.IdentifiableResource
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.util.Objects
import java.util.UUID

class CreateTaskBatchResource(
    override val id: UUID?,
    name: String,
    description: String?,
    location: String?,
    status: TaskStatusEnum,
    projectId: ProjectId,
    projectCraftId: ProjectCraftId,
    assigneeId: ParticipantId?,
    workAreaId: WorkAreaId?
) :
    SaveTaskResource(
        projectId, projectCraftId, name, description, location, status, assigneeId, workAreaId),
    IdentifiableResource {

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is CreateTaskBatchResource) {
      return false
    }
    return id == other.id
  }

  @ExcludeFromCodeCoverage override fun hashCode(): Int = Objects.hash(id)
}
