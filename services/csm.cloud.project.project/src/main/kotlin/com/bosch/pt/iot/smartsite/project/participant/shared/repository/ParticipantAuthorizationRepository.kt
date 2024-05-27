/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.shared.repository

import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Component
open class ParticipantAuthorizationRepository(
    private val participantAuthorizationCache: ParticipantAuthorizationCache,
    private val participantRepository: ParticipantRepository
) {

  open fun getParticipantsOfCurrentUser(
      projectIdentifiers: Set<ProjectId>
  ): Collection<ParticipantAuthorizationDto> =
      if (isWebRequestOrKafkaRequestScope)
          participantAuthorizationCache.getParticipantsOfCurrentUser(projectIdentifiers)
      else
          participantRepository.findByProjectIdentifierInAndUserIdentifierAndActiveTrue(
              projectIdentifiers, getCurrentUser().identifier!!)

  open fun getParticipantOfCurrentUser(projectIdentifier: ProjectId): ParticipantAuthorizationDto? =
      getParticipantsOfCurrentUser(setOf(projectIdentifier)).firstOrNull()

  private val isWebRequestOrKafkaRequestScope: Boolean
    get() = RequestContextHolder.getRequestAttributes() != null
}
