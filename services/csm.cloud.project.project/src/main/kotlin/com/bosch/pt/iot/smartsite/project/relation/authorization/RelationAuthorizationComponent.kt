/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.authorization

import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationAuthorizationDto
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class RelationAuthorizationComponent(
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository,
    private val relationRepository: RelationRepository
) {

  open fun hasCreateRelationPermissionOnProject(projectIdentifier: ProjectId) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasViewPermissionOnProject(projectIdentifier: ProjectId) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasDeletePermissionOnRelationOfProject(
      relationIdentifier: UUID,
      projectIdentifier: ProjectId
  ) = filterRelationsWithDeletePermission(setOf(relationIdentifier), projectIdentifier).isNotEmpty()

  open fun filterRelationsWithDeletePermission(
      relationIdentifiers: Collection<UUID>,
      projectIdentifier: ProjectId
  ): Set<UUID> {
    val currentParticipant = getCurrentParticipant(projectIdentifier) ?: return emptySet()
    return findRelationDtos(relationIdentifiers)
        .filter { hasParticipantDeletePermission(currentParticipant, it) }
        .map { it.relationIdentifier }
        .toSet()
  }

  private fun hasParticipantDeletePermission(
      participant: ParticipantAuthorizationDto,
      relation: RelationAuthorizationDto
  ) =
      when (participant.role) {
        CSM -> true
        CR -> relation.createdByCompanyOf(participant)
        FM -> relation.createdBy(participant)
      }

  private fun findRelationDtos(relationIdentifiers: Collection<UUID>) =
      relationRepository.findAllForAuthorizationByIdentifierIn(relationIdentifiers)

  private fun getCurrentParticipant(projectIdentifier: ProjectId) =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId) =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier) != null
}
