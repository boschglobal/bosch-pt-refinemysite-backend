/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.boundary

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.model.ParticipantMapping
import com.bosch.pt.csm.cloud.projectmanagement.project.repository.ParticipantMappingRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantMappingService(
    private val participantMappingRepository: ParticipantMappingRepository
) {

  @Trace
  fun saveParticipant(event: ParticipantEventG3Avro) {
    val aggregate = event.aggregate
    val projectIdentifier = aggregate.project.identifier.toUUID()
    val userIdentifier = aggregate.user.identifier.toUUID()
    val companyIdentifier = aggregate.company.identifier.toUUID()

    var mapping =
        participantMappingRepository.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, userIdentifier)

    if (mapping == null) {
      mapping =
          ParticipantMapping(
              projectIdentifier, companyIdentifier, aggregate.role.name, userIdentifier)
    } else {
      mapping.participantRole = aggregate.role.name
      mapping.companyIdentifier = companyIdentifier
    }

    participantMappingRepository.save(mapping)
  }

  @Trace
  fun deleteByProjectIdentifier(projectIdentifier: AggregateIdentifierAvro) {
    val participantMappings =
        participantMappingRepository.findAllByProjectIdentifier(
            projectIdentifier.identifier.toUUID())

    participantMappingRepository.deleteAllInBatch(participantMappings)
  }

  @Trace
  fun deleteByProjectIdentifierAndUserIdentifier(projectIdentifier: UUID, userIdentifier: UUID) =
      participantMappingRepository.deleteByProjectIdentifierAndUserIdentifier(
          projectIdentifier, userIdentifier)
}
