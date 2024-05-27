/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.model.ParticipantMapping
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ParticipantMappingRepository : JpaRepository<ParticipantMapping, Long> {

  fun findOneByProjectIdentifierAndUserIdentifier(
      projectIdentifier: UUID,
      userIdentifier: UUID
  ): ParticipantMapping?

  fun findAllByProjectIdentifierAndParticipantRole(
      projectIdentifier: UUID,
      participantRole: String
  ): Set<ParticipantMapping>

  fun findAllByProjectIdentifierAndParticipantRoleAndCompanyIdentifier(
      projectIdentifier: UUID,
      participantRole: String,
      companyIdentifier: UUID
  ): Set<ParticipantMapping>

  fun findAllByProjectIdentifier(projectIdentifier: UUID): List<ParticipantMapping>

  fun deleteByProjectIdentifierAndUserIdentifier(projectIdentifier: UUID, userIdentifier: UUID)
}
