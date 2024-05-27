/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.domain.RelationId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.Relation
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationTypeEnum
import java.util.UUID
import org.springframework.data.mongodb.repository.MongoRepository

interface RelationRepository : MongoRepository<Relation, RelationId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: RelationId): Relation?

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<Relation>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<Relation>

  fun findAllBySourceIdentifierInAndSourceTypeAndTargetTypeAndTypeAndDeletedFalse(
      taskIds: List<UUID>,
      sourceType: String,
      targetType: String,
      type: RelationTypeEnum
  ): List<Relation>

  fun findAllBySourceIdentifierInAndSourceTypeAndTargetTypeAndTypeNotAndDeletedFalse(
      taskIds: List<UUID>,
      sourceType: String,
      targetType: String,
      type: RelationTypeEnum
  ): List<Relation>

  fun findAllByTargetIdentifierInAndTargetTypeAndSourceTypeAndTypeNotAndDeletedFalse(
      taskIds: List<UUID>,
      targetType: String,
      sourceType: String,
      type: RelationTypeEnum
  ): List<Relation>

  fun findAllByTargetIdentifierInAndTypeAndDeletedFalse(
      identifier: List<UUID>,
      type: RelationTypeEnum
  ): List<Relation>
}
