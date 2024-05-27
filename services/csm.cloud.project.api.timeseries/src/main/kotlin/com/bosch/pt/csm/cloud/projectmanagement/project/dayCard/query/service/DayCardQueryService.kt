/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class DayCardQueryService(private val repository: DayCardRepository) {

  @Cacheable(cacheNames = ["daycards-by-tasks-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByTasksAndDeletedFalse(taskIds: List<TaskId>): List<DayCard> =
      repository.findAllByTaskInAndDeletedFalse(taskIds)

  @Cacheable(cacheNames = ["daycards-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<DayCard> =
      repository.findAllByProjectIn(projectIds)

  @Cacheable(cacheNames = ["daycards-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<DayCard> =
      repository.findAllByProjectInAndDeletedFalse(projectIds)
}
