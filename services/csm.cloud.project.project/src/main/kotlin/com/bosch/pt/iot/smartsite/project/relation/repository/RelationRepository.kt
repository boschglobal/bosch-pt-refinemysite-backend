/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.repository

import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationAuthorizationDto
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RelationRepository :
    KafkaStreamableRepository<Relation, Long, RelationEventEnumAvro>, RelationRepositoryExtension {

  fun existsByTypeAndSourceIdentifierAndSourceTypeAndTargetIdentifierAndTargetType(
      type: RelationTypeEnum,
      sourceIdentifier: UUID,
      sourceType: RelationElementTypeEnum,
      targetIdentifier: UUID,
      targetType: RelationElementTypeEnum
  ): Boolean

  fun findOneByIdentifierAndProjectIdentifier(
      identifier: UUID,
      projectIdentifier: ProjectId
  ): Relation?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifierAndProjectIdentifier(
      identifier: UUID,
      projectIdentifier: ProjectId
  ): Relation?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findAllWithDetailsByIdentifierInAndProjectIdentifier(
      identifiers: Collection<UUID>,
      projectIdentifier: ProjectId
  ): List<Relation>

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findAllWithDetailsByIdentifierIn(identifiers: Collection<UUID>): List<Relation>

  fun findAllByIdentifierIn(identifiers: Collection<UUID>): List<Relation>

  @Query(
      "select r from Relation r where r.source.identifier = :identifier or r.target.identifier = :identifier")
  fun findAllBySourceOrTarget(@Param("identifier") identifier: UUID): List<Relation>

  fun findAllByProjectId(projectId: Long): List<Relation>

  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<Relation>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationAuthorizationDto(" +
          "r.project.identifier, r.identifier, p.identifier, p.company.identifier) " +
          "from Relation r, Participant p " +
          "where p.user = r.createdBy " +
          "and r.identifier in :relationIdentifiers")
  fun findAllForAuthorizationByIdentifierIn(
      @Param("relationIdentifiers") relationIdentifiers: Collection<UUID>
  ): Set<RelationAuthorizationDto>
}
