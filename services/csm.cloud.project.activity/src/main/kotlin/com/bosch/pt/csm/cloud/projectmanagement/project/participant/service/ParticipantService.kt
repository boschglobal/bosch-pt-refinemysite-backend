/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.service

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.ParticipantRoleEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.ParticipantRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ParticipantService(
    @Value("\${testadmin.user.identifier}") private val testadminUserIdentifier: UUID,
    private val participantRepository: ParticipantRepository
) {

  @Trace fun save(participant: Participant) = participantRepository.save(participant)

  @Trace
  fun findOneByProjectIdentifierAndUserIdentifier(
      projectIdentifier: UUID,
      userIdentifier: UUID
  ): Participant {

    // This is a workaround used to deal with the data that was created by the testadmin user,
    // for which a participant does not exist and a null pointer exception will be throw
    if (userIdentifier == testadminUserIdentifier) {
      return createFakeParticipant(projectIdentifier, userIdentifier)
    }

    return participantRepository.findOneCachedByProjectIdentifierAndUserIdentifierAndActiveTrue(
        projectIdentifier, userIdentifier)
        ?: error(
            "Could not find active participant for user $userIdentifier in project $projectIdentifier")
  }

  @Trace
  fun findOneCacheByIdentifierAndProjectIdentifier(identifier: UUID, projectIdentifier: UUID) =
      participantRepository.findOneCachedByIdentifierAndProjectIdentifier(
          identifier, projectIdentifier)!!

  private fun createFakeParticipant(projectIdentifier: UUID, userIdentifier: UUID) =
      Participant(
          identifier = FAKE_PARTICIPANT_IDENTIFIER,
          projectIdentifier = projectIdentifier,
          role = ParticipantRoleEnum.FM,
          companyIdentifier = FAKE_IDENTIFIER,
          userIdentifier = userIdentifier)

  companion object {
    val FAKE_IDENTIFIER: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val FAKE_PARTICIPANT_IDENTIFIER = FAKE_IDENTIFIER
  }
}
