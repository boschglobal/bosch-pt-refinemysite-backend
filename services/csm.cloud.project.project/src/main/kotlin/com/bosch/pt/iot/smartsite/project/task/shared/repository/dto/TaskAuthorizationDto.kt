/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.repository.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import java.util.UUID

data class TaskAuthorizationDto(
    // Take care: don't reorder these attributes! A Hibernate projection relies on this ordering.
    val taskIdentifier: TaskId,
    val taskStatus: TaskStatusEnum,
    val projectIdentifier: ProjectId,
    val assigneeIdentifier: ParticipantId?,
    val assigneeCompanyIdentifier: UUID?,
    val createdByUserIdentifier: UserId?,
    val createdByCompanyIdentifier: UUID?
) {

  fun isAssignedTo(participant: ParticipantAuthorizationDto): Boolean =
      assigneeIdentifier != null && assigneeIdentifier == participant.identifier

  fun isAssignedToCompanyOf(participant: ParticipantAuthorizationDto): Boolean =
      assigneeCompanyIdentifier != null &&
          assigneeCompanyIdentifier == participant.companyIdentifier

  fun isClosed(): Boolean = taskStatus === CLOSED

  fun isAccepted(): Boolean = taskStatus === ACCEPTED

  // null check only required because some tasks in production are created by an admin user who
  // doesn't have an employee.
  fun isCreatedBy(participant: ParticipantAuthorizationDto): Boolean =
      createdByUserIdentifier != null &&
          createdByUserIdentifier == participant.userIdentifier.asUserId()

  // null check only required because some tasks in production are created by an admin user who
  // doesn't have an employee.
  fun isCreatedByCompanyOf(participant: ParticipantAuthorizationDto): Boolean =
      createdByCompanyIdentifier != null &&
          createdByCompanyIdentifier == participant.companyIdentifier

  fun isUnassigned(): Boolean = assigneeCompanyIdentifier == null
}
