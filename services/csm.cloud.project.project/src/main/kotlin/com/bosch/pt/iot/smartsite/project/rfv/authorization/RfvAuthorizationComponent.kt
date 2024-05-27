/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.authorization

import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class RfvAuthorizationComponent(
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {

  open fun hasUpdateRfvPermissionOnProject(projectIdentifier: ProjectId) =
      isCurrentUserCsmOfProject(projectIdentifier)

  open fun hasViewPermissionOnRfv(projectIdentifier: ProjectId) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun getProjectsWithUpdateRfvPermissions(projectIdentifiers: Set<ProjectId>): Set<ProjectId> =
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .filter { it.role == CSM }
          .map { it.projectIdentifier }
          .toSet()

  private fun isCurrentUserCsmOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)?.role == CSM

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId?) =
      projectIdentifier != null &&
          participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier) != null
}
