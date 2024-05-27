/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import java.util.UUID
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ParticipantRepository :
    MongoRepository<Participant, UUID>, ShardedSaveOperation<Participant, UUID> {

  @Query("{'_class': Participant}") override fun findAll(): List<Participant>

  @DeleteQuery("{'_class': Participant}") override fun deleteAll()

  @Cacheable(cacheNames = ["participant"])
  fun findOneCachedByIdentifierAndProjectIdentifier(
      identifier: UUID,
      projectIdentifier: UUID
  ): Participant?

  @Cacheable(cacheNames = ["participant-user"])
  fun findOneCachedByProjectIdentifierAndUserIdentifierAndActiveTrue(
      projectIdentifier: UUID,
      userIdentifier: UUID
  ): Participant?
}
