/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service

import com.bosch.pt.csm.cloud.projectmanagement.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class ParticipantQueryService(private val participantRepository: ParticipantRepository) {

  @PreAuthorize("isAuthenticated()")
  @PostAuthorize("@participantAuthorizationComponent.hasReadPermissionOnProjects(returnObject)")
  fun findActiveParticipantsOfCurrentUser(): List<Participant> =
      participantRepository.findAllByUserAndStatus(
          SecurityContextHelper.getCurrentUserDetails().userIdentifier().asUserId(), ACTIVE)

  @PreAuthorize("isAuthenticated()")
  fun findActiveParticipantsOfProjects(projectIds: List<ProjectId>): List<Participant> =
      participantRepository.findAllByProjectInAndStatus(projectIds, ACTIVE)

  @PreAuthorize("isAuthenticated()")
  fun findParticipantsOfProjects(projectIds: List<ProjectId>): List<Participant> =
      participantRepository.findAllByProjectIn(projectIds)
}
