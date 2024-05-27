/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.repository

import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils
import com.bosch.pt.iot.smartsite.common.repository.AbstractCache
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class ParticipantAuthorizationCache(private val participantRepository: ParticipantRepository) :
    AbstractCache<ProjectId, ParticipantAuthorizationDto>() {

  open fun getParticipantsOfCurrentUser(
      projectIdentifiers: Set<ProjectId>
  ): Collection<ParticipantAuthorizationDto> = get(projectIdentifiers)

  open fun getParticipantOfCurrentUser(projectIdentifier: ProjectId): ParticipantAuthorizationDto? =
      get(setOf(projectIdentifier)).firstOrNull()

  override fun loadFromDatabase(
      projectIdentifiers: Set<ProjectId>
  ): Set<ParticipantAuthorizationDto> =
      participantRepository.findByProjectIdentifierInAndUserIdentifierAndActiveTrue(
          projectIdentifiers, AuthorizationUtils.getCurrentUser().identifier!!)

  override fun getCacheKey(participant: ParticipantAuthorizationDto): ProjectId =
      participant.projectIdentifier
}
