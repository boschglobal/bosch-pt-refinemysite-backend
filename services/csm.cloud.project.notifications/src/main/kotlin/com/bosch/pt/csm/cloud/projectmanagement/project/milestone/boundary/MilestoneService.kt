/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.boundary

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.MilestoneRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class MilestoneService(private val milestoneRepository: MilestoneRepository) {

  @Trace fun save(milestone: Milestone): Milestone = milestoneRepository.save(milestone)

  @Trace
  fun deleteMilestone(identifier: UUID, projectIdentifier: UUID) =
      milestoneRepository.deleteMilestone(identifier, projectIdentifier)

  @Trace
  fun updatePosition(
      projectIdentifier: UUID,
      aggregateIdentifier: AggregateIdentifier,
      position: Int
  ) = milestoneRepository.updatePosition(projectIdentifier, aggregateIdentifier, position)

  @Trace
  fun findMilestone(projectIdentifier: UUID, aggregateIdentifier: AggregateIdentifier) =
      milestoneRepository.findMilestone(projectIdentifier, aggregateIdentifier)
}
