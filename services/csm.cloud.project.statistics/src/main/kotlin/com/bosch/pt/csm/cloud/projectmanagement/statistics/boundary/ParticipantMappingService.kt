/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.projectmanagement.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ParticipantMapping
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantMappingService(
    private val participantMappingRepository: ParticipantMappingRepository
) {

  @Trace
  @Transactional(readOnly = true)
  fun findOneByParticipantIdentifier(participantIdentifier: UUID) =
      participantMappingRepository.findOneByParticipantIdentifier(participantIdentifier)

  @Trace
  @Transactional
  fun saveParticipant(event: ParticipantEventG3Avro) {

    val aggregate = event.getAggregate()
    val projectIdentifier = aggregate.getProject().toUUID()
    val userIdentifier = aggregate.getUser().toUUID()
    val companyIdentifier = aggregate.getCompany().toUUID()
    val participantIdentifier = aggregate.getAggregateIdentifier().toUUID()

    val mapping =
        participantMappingRepository.findOneByParticipantIdentifier(participantIdentifier)?.also {
          it.participantRole = aggregate.getRole().name
          it.companyIdentifier = companyIdentifier
          it.userIdentifier = userIdentifier
          it.projectIdentifier = projectIdentifier
          it.active = event.getName() != ParticipantEventEnumAvro.DEACTIVATED
        }
            ?: ParticipantMapping(
                participantIdentifier,
                projectIdentifier,
                aggregate.getRole().name,
                userIdentifier,
                companyIdentifier)

    participantMappingRepository.save(mapping)
  }

  @Trace
  @Transactional
  fun deleteAllByProjectIdentifier(projectIdentifier: UUID?) =
      participantMappingRepository.deleteAllByProjectIdentifier(projectIdentifier!!)

  @Trace
  @Transactional(readOnly = true)
  fun findParticipantMappingByProjectAndCurrentUser(projectIdentifier: UUID) =
      participantMappingRepository.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
          projectIdentifier, getCurrentUser().identifier)
}
