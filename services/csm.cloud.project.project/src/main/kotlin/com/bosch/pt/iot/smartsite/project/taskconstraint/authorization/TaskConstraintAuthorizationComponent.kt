/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.authorization

import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class TaskConstraintAuthorizationComponent(
    private val participantRepository: ParticipantAuthorizationRepository
) {

  open fun hasUpdateConstraintPermissionOnProject(projectIdentifier: ProjectId) =
      isCurrentUserCsmOfProject(projectIdentifier)

  open fun hasViewPermissionOnConstraint(projectIdentifier: ProjectId) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun getProjectsWithUpdateConstraintPermissions(
      projectIdentifiers: Set<ProjectId>
  ): Set<ProjectId> =
      participantRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .filter { it.role == CSM }
          .map { it.projectIdentifier }
          .toSet()

  private fun isCurrentUserCsmOfProject(projectIdentifier: ProjectId): Boolean =
      participantRepository.getParticipantOfCurrentUser(projectIdentifier)?.role == CSM

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId?) =
      projectIdentifier != null &&
          participantRepository.getParticipantOfCurrentUser(projectIdentifier) != null
}
