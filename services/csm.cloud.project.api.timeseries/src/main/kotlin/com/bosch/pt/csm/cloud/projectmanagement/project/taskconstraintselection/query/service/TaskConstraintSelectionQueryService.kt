/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.ProjectTaskConstraintSelections
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelection
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.repository.TaskConstraintSelectionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TaskConstraintSelectionQueryService(
    private val taskConstraintSelectionRepository: TaskConstraintSelectionRepository
) {

  @Cacheable(cacheNames = ["task-constraint-selection-by-tasks-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByTasksAndDeletedFalse(
      taskIds: List<TaskId>
  ): Map<TaskId, List<TaskConstraintSelection>> =
      taskConstraintSelectionRepository.findAllByTaskInAndDeletedFalse(taskIds).groupBy { it.task }

  @Cacheable(cacheNames = ["task-constraint-selection-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<ProjectTaskConstraintSelections> =
      taskConstraintSelectionRepository
          .findAllByProjectIn(projectIds)
          .groupBy { it.project }
          .flatMap { projectEntry ->
            val projectId = projectEntry.key
            projectEntry.value
                .groupBy { it.task }
                .map { ProjectTaskConstraintSelections(projectId, it.key, it.value) }
          }

  @Cacheable(cacheNames = ["task-constraint-selection-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(
      projectIds: List<ProjectId>
  ): List<ProjectTaskConstraintSelections> =
      taskConstraintSelectionRepository
          .findAllByProjectInAndDeletedFalse(projectIds)
          .groupBy { it.project }
          .flatMap { projectEntry ->
            val projectId = projectEntry.key
            projectEntry.value
                .groupBy { it.task }
                .map { ProjectTaskConstraintSelections(projectId, it.key, it.value) }
          }
}
