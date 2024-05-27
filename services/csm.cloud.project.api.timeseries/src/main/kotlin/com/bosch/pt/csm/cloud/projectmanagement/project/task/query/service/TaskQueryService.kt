/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.repository.TaskRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TaskQueryService(private val taskRepository: TaskRepository) {

  @Cacheable(cacheNames = ["tasks-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<Task> =
      taskRepository.findAllByProjectInAndDeletedFalse(projectIds)

  @Cacheable(cacheNames = ["tasks-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<Task> =
      taskRepository.findAllByProjectIn(projectIds)

  @Cacheable(cacheNames = ["tasks-by-identifiers"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiersAndDeletedFalse(taskIds: List<TaskId>) =
      taskRepository.findAllByIdentifierInAndDeletedFalse(taskIds)
}
