/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.service

import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskService(private val taskRepository: TaskRepository) {

  @Trace fun save(task: Task) = taskRepository.save(task)

  @Trace
  fun find(identifier: UUID, version: Long, projectIdentifier: UUID) =
      taskRepository.find(identifier, version, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      taskRepository.deleteTaskAndAllRelatedDocuments(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      taskRepository.deleteByVersion(identifier, version, projectIdentifier)
}
