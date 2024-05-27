/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.repository.TopicRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TopicQueryService(private val topicRepository: TopicRepository) {

  @Cacheable(cacheNames = ["topics-by-tasks"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByTasksAndDeletedFalse(taskIds: List<TaskId>): List<Topic> =
      topicRepository.findAllByTaskInAndDeletedFalse(taskIds)

  @Cacheable(cacheNames = ["topics-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findTopicsOfProjects(projectIds: List<ProjectId>): List<Topic> =
      topicRepository.findAllByProjectIn(projectIds)

  @Cacheable(cacheNames = ["topics-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findTopicsOfProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<Topic> =
    topicRepository.findAllByProjectInAndDeletedFalse(projectIds)
}
