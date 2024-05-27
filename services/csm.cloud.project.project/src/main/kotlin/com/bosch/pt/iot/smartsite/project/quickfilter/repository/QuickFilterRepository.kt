/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.repository

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import java.util.UUID
import org.springframework.data.mongodb.repository.MongoRepository

interface QuickFilterRepository : MongoRepository<QuickFilter, UUID> {

  fun findAllByParticipantIdentifierAndProjectIdentifierOrderByNameAsc(
      participantIdentifier: ParticipantId,
      projectRef: ProjectId
  ): List<QuickFilter>

  fun findOneByIdentifierAndProjectIdentifier(
      identifier: QuickFilterId,
      projectRef: ProjectId
  ): QuickFilter?

  fun countAllByParticipantIdentifierAndProjectIdentifier(
      participantIdentifier: ParticipantId,
      projectRef: ProjectId
  ): Long

  fun existsOneByIdentifierAndParticipantIdentifierAndProjectIdentifier(
      identifier: QuickFilterId,
      participantIdentifier: ParticipantId,
      projectRef: ProjectId
  ): Boolean

  fun deleteByIdentifierAndProjectIdentifier(identifier: QuickFilterId, projectRef: ProjectId)

  fun deleteAllByProjectIdentifier(projectRef: ProjectId)
}
