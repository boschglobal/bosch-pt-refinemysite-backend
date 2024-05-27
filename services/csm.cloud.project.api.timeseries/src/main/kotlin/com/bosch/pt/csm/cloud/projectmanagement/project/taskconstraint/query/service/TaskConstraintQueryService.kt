/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.ProjectTaskConstraints
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraint
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.repository.TaskConstraintRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TaskConstraintQueryService(private val taskConstraintRepository: TaskConstraintRepository) {

  @Cacheable(cacheNames = ["task-constraints-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(
      projectIds: List<ProjectId>
  ): Map<ProjectId, List<TaskConstraint>> =
      taskConstraintRepository.findAllByProjectInAndDeletedFalse(projectIds).groupBy { it.project }

  @Cacheable(cacheNames = ["task-constraints-by-projects-with-missing"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsWithMissing(projectIds: List<ProjectId>): List<ProjectTaskConstraints> =
      taskConstraintRepository
          .findAllByProjectIn(projectIds)
          .groupBy { it.project }
          .map { ProjectTaskConstraints(it.key, it.value) }
          .let {
            val missingProjects = projectIds - it.map { it.projectId }.toSet()
            it + missingProjects.map { ProjectTaskConstraints(it, emptyList()) }
          }

  @Cacheable(cacheNames = ["task-constraints-by-projects-deleted-false-with-missing"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalseWithMissing(
      projectIds: List<ProjectId>
  ): List<ProjectTaskConstraints> =
      taskConstraintRepository
          .findAllByProjectInAndDeletedFalse(projectIds)
          .groupBy { it.project }
          .map { ProjectTaskConstraints(it.key, it.value) }
          .let {
            val missingProjects = projectIds - it.map { it.projectId }.toSet()
            it + missingProjects.map { ProjectTaskConstraints(it, emptyList()) }
          }
}
