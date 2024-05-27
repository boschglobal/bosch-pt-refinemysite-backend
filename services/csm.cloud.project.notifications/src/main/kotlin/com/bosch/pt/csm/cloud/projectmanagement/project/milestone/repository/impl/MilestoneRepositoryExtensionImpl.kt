/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.impl

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonCriteriaSnippets.matchesAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator.and
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets.belongsToProject
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.MilestoneRepositoryExtension
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update.update

class MilestoneRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    MilestoneRepositoryExtension {

  override fun deleteMilestone(identifier: UUID, projectIdentifier: UUID): DeleteResult =
      mongoOperations.remove(
          query(and(belongsToProject(projectIdentifier), isAnyVersionOfMilestone(identifier))),
          PROJECT_STATE)

  override fun updatePosition(
      projectIdentifier: UUID,
      aggregateIdentifier: AggregateIdentifier,
      position: Int
  ): UpdateResult =
      mongoOperations.updateFirst(
          query(
              and(
                  belongsToProject(projectIdentifier),
                  matchesAggregateIdentifier(aggregateIdentifier))),
          update("position", position),
          Milestone::class.java)

  override fun findMilestones(projectIdentifier: UUID): List<Milestone> =
      mongoOperations.find(
          query(and(belongsToProject(projectIdentifier), isMilestone()))
              .with(Sort.by(DESC, "identifier", "version")),
          PROJECT_STATE)

  override fun findMilestone(
      projectIdentifier: UUID,
      aggregateIdentifier: AggregateIdentifier
  ): Milestone? =
      mongoOperations.findOne(
          query(
              and(
                  belongsToProject(projectIdentifier),
                  matchesAggregateIdentifier(aggregateIdentifier))),
          Milestone::class.java)

  private fun isAnyVersionOfMilestone(identifier: UUID): Criteria =
      isMilestone().and(ID_IDENTIFIER).`is`(identifier)

  private fun isMilestone(): Criteria = where(ID_TYPE).`is`(ID_TYPE_VALUE_MILESTONE)
}
