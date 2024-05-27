/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskService(private val taskRepository: TaskRepository) {

  @Trace fun save(task: Task) = taskRepository.save(task)

  @Trace
  fun findDisplayName(taskIdentifier: UUID, projectIdentifier: UUID) =
      taskRepository.findDisplayName(taskIdentifier, projectIdentifier)

  @Trace
  fun findLatest(taskIdentifier: UUID, projectIdentifier: UUID) =
      taskRepository.findLatest(taskIdentifier, projectIdentifier)

  @Trace
  fun find(taskIdentifier: UUID, version: Long, projectIdentifier: UUID) =
      taskRepository.find(taskIdentifier, version, projectIdentifier)

  @Trace
  fun findAssigneeOfTaskWithVersion(taskIdentifier: UUID, version: Long, projectIdentifier: UUID) =
      taskRepository.findAssigneeOfTaskWithVersion(taskIdentifier, version, projectIdentifier)

  @Trace
  fun deleteTaskAndAllRelatedDocuments(taskIdentifier: UUID, projectIdentifier: UUID) =
      taskRepository.deleteTaskAndAllRelatedDocuments(taskIdentifier, projectIdentifier)
}
