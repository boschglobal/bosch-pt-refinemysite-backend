/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ParticipantMapping
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ParticipantMappingRepository : JpaRepository<ParticipantMapping, Long> {

  fun findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
      projectIdentifier: UUID,
      userIdentifier: UUID
  ): ParticipantMapping?

  fun findOneByParticipantIdentifier(participantIdentifier: UUID): ParticipantMapping?

  fun findAllByProjectIdentifier(projectIdentifier: UUID): List<ParticipantMapping>

  fun deleteAllByProjectIdentifier(projectIdentifier: UUID)
}
