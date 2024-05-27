/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskstatistics.boundary

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatistics
import com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatisticsEntry
import com.bosch.pt.iot.smartsite.project.taskstatistics.repository.TaskStatisticsRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskStatisticsService(private val taskStatisticsRepository: TaskStatisticsRepository) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  open fun findTaskStatistics(taskIdentifiers: Set<TaskId>): Map<TaskId, TaskStatistics> {
    if (taskIdentifiers.isEmpty()) {
      return emptyMap()
    }

    return TaskStatisticsEntry.toMap(taskStatisticsRepository.getTaskStatistics(taskIdentifiers))
  }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  open fun findTaskStatistics(taskIdentifier: TaskId): TaskStatistics =
      findTaskStatistics(setOf(taskIdentifier))[taskIdentifier] ?: TaskStatistics(0L, 0L)
}
