/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.ParticipantRoleEnum
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.UUID

interface ParticipantRepository :
    MongoRepository<Participant, UUID>,
    ShardedSaveOperation<Participant, UUID>,
    ParticipantRepositoryExtension {

    fun findAllByProjectIdentifierAndRole(projectIdentifier: UUID, role: ParticipantRoleEnum): List<Participant>

    fun findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
        projectIdentifier: UUID,
        userIdentifier: UUID
    ): Participant?

    fun findFirstByProjectIdentifierAndUserIdentifier(
        projectIdentifier: UUID,
        userIdentifier: UUID
    ): Participant

    @Cacheable(cacheNames = ["participant-user"])
    fun findOneCachedByProjectIdentifierAndUserIdentifierAndActiveTrue(
        projectIdentifier: UUID,
        userIdentifier: UUID
    ): Participant?

    fun findOneByIdentifierAndProjectIdentifier(identifier: UUID, projectIdentifier: UUID): Participant

    @Cacheable(cacheNames = ["participant"])
    fun findOneCachedByIdentifierAndProjectIdentifier(
        identifier: UUID,
        projectIdentifier: UUID
    ): Participant

    fun findAllByProjectIdentifierAndCompanyIdentifierAndRole(
        projectIdentifier: UUID,
        companyIdentifier: UUID,
        role: ParticipantRoleEnum
    ): List<Participant>
}
