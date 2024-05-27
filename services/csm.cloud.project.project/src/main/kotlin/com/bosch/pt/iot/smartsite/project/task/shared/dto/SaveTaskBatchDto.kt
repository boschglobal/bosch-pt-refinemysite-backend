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
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.util.Objects

class SaveTaskBatchDto(
    val id: TaskId,
    val version: Long?,
    name: String,
    description: String?,
    location: String?,
    status: TaskStatusEnum,
    projectCraftIdentifier: ProjectCraftId,
    assigneeIdentifier: ParticipantId?,
    workAreaIdentifier: WorkAreaId?
) :
    SaveTaskDto(
        name,
        description,
        location,
        status,
        projectCraftIdentifier,
        assigneeIdentifier,
        workAreaIdentifier) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is SaveTaskBatchDto) {
      return false
    }
    return id == other.id && version == other.version
  }

  override fun hashCode(): Int = Objects.hash(id, version)
}
