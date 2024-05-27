/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.ParticipantRoleEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.ParticipantRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class ParticipantService(private val participantRepository: ParticipantRepository) {

  @Trace
  fun findAllByProjectIdentifierAndRole(projectIdentifier: UUID, role: ParticipantRoleEnum) =
      participantRepository.findAllByProjectIdentifierAndRole(projectIdentifier, role)

  @Trace
  fun findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
      projectIdentifier: UUID,
      userIdentifier: UUID
  ) =
      participantRepository.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
          projectIdentifier, userIdentifier)

  /**
   * Since we reference the user and not the participant in the auditing information we cannot
   * always identify the right participant for a project. This scenario can occur, if a user changed
   * companies while assigned to a project. When a notification is rendered, where the user become
   * inactive in that project, the fallback is used here
   */
  @Trace
  fun findOneCachedByProjectIdentifierAndUserIdentifier(
      projectIdentifier: UUID,
      userIdentifier: UUID
  ) =
      participantRepository.findOneCachedByProjectIdentifierAndUserIdentifierAndActiveTrue(
          projectIdentifier, userIdentifier)
          ?: participantRepository.findFirstByProjectIdentifierAndUserIdentifier(
              projectIdentifier, userIdentifier)

  @Trace
  fun findOneByIdentifierAndProjectIdentifier(
      participantIdentifier: UUID,
      projectIdentifier: UUID
  ) =
      participantRepository.findOneByIdentifierAndProjectIdentifier(
          participantIdentifier, projectIdentifier)

  @Trace
  fun findOneCachedByIdentifierAndProjectIdentifier(
      participantIdentifier: UUID,
      projectIdentifier: UUID
  ) =
      participantRepository.findOneCachedByIdentifierAndProjectIdentifier(
          participantIdentifier, projectIdentifier)

  @Trace
  fun findCrsForCompany(projectIdentifier: UUID, companyIdentifier: UUID) =
      participantRepository.findAllByProjectIdentifierAndCompanyIdentifierAndRole(
          projectIdentifier, companyIdentifier, ParticipantRoleEnum.CR)

  @Trace
  fun findAllByProjectIdentifier(projectIdentifier: UUID) =
      participantRepository.findAllByProjectIdentifier(projectIdentifier)

  @Trace fun save(participant: Participant) = participantRepository.save(participant)
}
