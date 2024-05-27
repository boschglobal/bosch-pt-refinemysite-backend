/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.repository.TaskScheduleRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TaskScheduleQueryService(private val repository: TaskScheduleRepository) {

  @Cacheable(cacheNames = ["task-schedules-by-tasks"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByTasks(taskIds: List<TaskId>): List<TaskSchedule> =
      repository.findAllByTaskIn(taskIds)

  @Cacheable(cacheNames = ["task-schedules-by-tasks-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByTasksAndDeletedFalse(taskIds: List<TaskId>): List<TaskSchedule> =
      repository.findAllByTaskInAndDeletedFalse(taskIds)

  @Cacheable(cacheNames = ["task-schedules-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<TaskSchedule> =
      repository.findAllByProjectIn(projectIds)
}
