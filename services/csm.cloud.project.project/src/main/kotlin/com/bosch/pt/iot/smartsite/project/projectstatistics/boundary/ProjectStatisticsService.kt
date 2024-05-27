/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectstatistics.boundary

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.StatisticsEntry
import com.bosch.pt.iot.smartsite.project.projectstatistics.repository.ProjectStatisticsRepository
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectStatisticsService(
    private val projectStatisticsRepository: ProjectStatisticsRepository
) {

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProjects(#projectIdentifiers)")
  @Transactional(readOnly = true)
  open fun getTaskStatistics(
      projectIdentifiers: Set<ProjectId>
  ): Map<ProjectId, Map<TaskStatusEnum, Long>> =
      if (projectIdentifiers.isEmpty()) {
        emptyMap()
      } else {
        StatisticsEntry.toMap(projectStatisticsRepository.getTaskStatistics(projectIdentifiers))
      }

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun getTaskStatistics(projectIdentifier: ProjectId): Map<TaskStatusEnum, Long> =
      getTaskStatistics(setOf(projectIdentifier))[projectIdentifier] ?: emptyMap()

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProjects(#projectIdentifiers)")
  @Transactional(readOnly = true)
  open fun getTopicStatistics(
      projectIdentifiers: Set<ProjectId>
  ): Map<ProjectId, Map<TopicCriticalityEnum, Long>> =
      if (projectIdentifiers.isEmpty()) {
        emptyMap()
      } else {
        StatisticsEntry.toMap(projectStatisticsRepository.getTopicStatistics(projectIdentifiers))
      }

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun getTopicStatistics(projectIdentifier: ProjectId): Map<TopicCriticalityEnum, Long> =
      getTopicStatistics(setOf(projectIdentifier))[projectIdentifier] ?: emptyMap()
}
