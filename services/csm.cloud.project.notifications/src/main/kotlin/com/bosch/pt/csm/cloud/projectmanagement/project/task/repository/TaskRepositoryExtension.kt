/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import java.util.UUID
import org.springframework.cache.annotation.Cacheable

interface TaskRepositoryExtension {

  fun find(identifier: UUID, version: Long, projectIdentifier: UUID): Task

  fun findLatest(identifier: UUID, projectIdentifier: UUID): Task

  fun findTasks(projectIdentifier: UUID): List<Task>

  @Cacheable(cacheNames = ["task-display-name"])
  fun findDisplayName(identifier: UUID, projectIdentifier: UUID): String?

  fun findAssigneeOfTaskWithVersion(
      taskIdentifier: UUID,
      version: Long,
      projectIdentifier: UUID
  ): UUID?

  fun deleteTaskAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID)
}
