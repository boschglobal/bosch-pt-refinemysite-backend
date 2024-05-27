/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.repository.MilestoneRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class MilestoneQueryService(private val milestoneRepository: MilestoneRepository) {

  @Cacheable(cacheNames = ["milestones-by-identifiers-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiersAndDeletedFalse(milestoneIds: List<MilestoneId>): List<Milestone> {
    return milestoneRepository.findAllByIdentifierInAndDeletedFalse(milestoneIds)
  }

  @Cacheable(cacheNames = ["milestones-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<Milestone> =
      milestoneRepository.findAllByProjectInAndDeletedIsFalse(projectIds)

  @Cacheable(cacheNames = ["milestones-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<Milestone> =
      milestoneRepository.findAllByProjectIn(projectIds)
}
