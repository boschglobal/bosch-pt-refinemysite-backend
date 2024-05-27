/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import java.util.UUID

interface MilestoneRepositoryExtension {
  fun deleteMilestone(identifier: UUID, projectIdentifier: UUID): DeleteResult

  fun updatePosition(
      projectIdentifier: UUID,
      aggregateIdentifier: AggregateIdentifier,
      position: Int
  ): UpdateResult

  fun findMilestones(projectIdentifier: UUID): List<Milestone>

  fun findMilestone(projectIdentifier: UUID, aggregateIdentifier: AggregateIdentifier): Milestone?
}
