/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.service

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.MilestoneRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class MilestoneService(private val milestoneRepository: MilestoneRepository) {

  @Trace fun save(milestone: Milestone) = milestoneRepository.save(milestone)

  @Trace
  fun updatePosition(
      projectIdentifier: UUID,
      aggregateIdentifier: AggregateIdentifier,
      position: Int
  ) = milestoneRepository.updatePosition(projectIdentifier, aggregateIdentifier, position)

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      milestoneRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun findMilestone(identifier: UUID, projectIdentifier: UUID, version: Long) =
      milestoneRepository.find(identifier, version, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      milestoneRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      milestoneRepository.deleteByVersion(identifier, version, projectIdentifier)
}
