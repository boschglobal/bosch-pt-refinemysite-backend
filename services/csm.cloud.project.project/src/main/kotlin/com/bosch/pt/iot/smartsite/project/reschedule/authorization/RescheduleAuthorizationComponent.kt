/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.authorization

import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class RescheduleAuthorizationComponent(
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository,
) {
  open fun hasReschedulePermissionOnProject(projectIdentifier: ProjectId): Boolean {
    val participant: ParticipantAuthorizationDto =
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)
            ?: return false

    return participant.role == ParticipantRoleEnum.CSM
  }

  open fun getProjectsWithReschedulePermissions(
      projectIdentifiers: Set<ProjectId>
  ): Set<ProjectId> =
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .filter { it.role == ParticipantRoleEnum.CSM }
          .map { it.projectIdentifier }
          .toSet()
}
