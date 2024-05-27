/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository

import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule

interface TaskScheduleRepositoryExtension {

  fun findOneByIdentifier(identifier: TaskScheduleId): TaskSchedule?

  /**
   * Returns a list of task schedule IDs for the given list of task IDs. Partitions the list of task
   * IDs automatically.
   *
   * @param taskIds list of task IDs
   * @return the list of topic schedule IDs
   */
  fun getIdsByTaskIdsPartitioned(taskIds: List<Long>): List<Long>

  /**
   * Deletes the schedule slots of the schedules with IDs in the specified list of schedule IDs.
   * Partitions the list of schedule IDs automatically.
   *
   * @param taskScheduleIds the list of task schedule IDs
   */
  fun deleteScheduleSlotsPartitioned(taskScheduleIds: List<Long>)

  /**
   * Deletes objects in partitioned.
   *
   * @param ids list of IDs to delete
   */
  fun deletePartitioned(ids: List<Long>)
}
