/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.authorization

import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class TaskCopyAuthorizationComponent(
    val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {

  open fun hasCopyTaskPermissionOnProject(projectIdentifier: ProjectId) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId?) =
      projectIdentifier != null &&
          participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier) != null
}
