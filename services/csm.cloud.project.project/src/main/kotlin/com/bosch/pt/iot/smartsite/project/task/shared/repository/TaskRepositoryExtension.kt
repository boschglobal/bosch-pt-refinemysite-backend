/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.repository

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto
import org.springframework.data.domain.Pageable

interface TaskRepositoryExtension {

  /**
   * Queries the database for tasks and applies filtering criteria appropriately.
   *
   * @param filters the criteria to filter on.
   * @return page with identifiers of all tasks that match all the filter criteria.
   */
  fun findTaskIdentifiersForFilters(filters: TaskFilterDto, pageable: Pageable): List<TaskId>

  fun findTaskIdsForFilters(filters: TaskFilterDto, pageable: Pageable): List<Long>

  fun countAllForFilters(taskFilters: TaskFilterDto): Long

  /**
   * Deletes objects in partitioned.
   *
   * @param ids list of IDs to delete
   */
  fun deletePartitioned(ids: List<Long>)

  /**
   * Marks a task as deleted without sending an event.
   *
   * @param taskId the task id
   */
  fun markAsDeleted(taskId: Long)

  /**
   * Marks the tasks as deleted without sending an event.
   *
   * @param identifiers list of the task identifiers
   */
  fun markAsDeleted(identifiers: List<Long>)
}
