/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.repository

interface DayCardRepositoryExtension {

  /**
   * Returns a list of day card IDs for the given list of task schedule IDs. Partitions the list of
   * task schedule IDs automatically.
   *
   * @param taskScheduleIds list of task schedule IDs
   * @return the list of day card IDs
   */
  fun getIdsByTaskScheduleIdsPartitioned(taskScheduleIds: List<Long>): List<Long>

  /**
   * Deletes objects in partitioned.
   *
   * @param ids list of IDs to delete
   */
  fun deletePartitioned(ids: List<Long>)
}
