/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.authorization

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class ParticipantAuthorizationComponent(
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository,
    private val participantRepository: ParticipantRepository,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
) {

  open fun hasAssignPermissionOnParticipantsOfProject(projectIdentifier: ProjectId): Boolean =
      isCurrentUserCsmOfProject(projectIdentifier)

  open fun hasReadPermissionOnParticipantsOfProjects(projectIdentifiers: Set<ProjectId>): Boolean =
      projectAuthorizationComponent.hasReadPermissionOnProjects(projectIdentifiers)

  open fun hasReadPermissionOnParticipantsOfProject(projectIdentifier: ProjectId): Boolean =
      hasReadPermissionOnParticipantsOfProjects(setOf(projectIdentifier))

  open fun hasReadPermissionOnParticipant(participantId: ParticipantId): Boolean =
      participantRepository.findProjectIdentifierByParticipantIdentifier(participantId).let {
        it != null && projectAuthorizationComponent.hasReadPermissionOnProject(it)
      }

  open fun hasUpdateAndDeletePermissionOnParticipantsOfProject(
      projectIdentifier: ProjectId
  ): Boolean = isCurrentUserCsmOfProject(projectIdentifier)

  open fun hasUpdatePermissionOnParticipant(participantId: ParticipantId): Boolean =
      participantRepository.findProjectIdentifierByParticipantIdentifier(participantId).let {
        it != null && isCurrentUserCsmOfProject(it)
      }

  open fun hasDeletePermissionOnParticipant(participantId: ParticipantId): Boolean =
      participantRepository.findProjectIdentifierByParticipantIdentifier(participantId).let {
        it != null && isCurrentUserCsmOfProject(it)
      }

  private fun isCurrentUserCsmOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)?.role == CSM
}
