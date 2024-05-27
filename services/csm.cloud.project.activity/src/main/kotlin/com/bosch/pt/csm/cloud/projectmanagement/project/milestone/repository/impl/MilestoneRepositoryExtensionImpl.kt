/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.impl

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonCriteriaSnippets.matchesAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator.and
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets.belongsToProject
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.MilestoneRepositoryExtension
import com.mongodb.client.result.UpdateResult
import java.util.UUID
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update.update

class MilestoneRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    MilestoneRepositoryExtension {

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
}
