/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.repository

interface TaskConstraintSelectionRepositoryExtension {

  /**
   * Returns a list of task constraint selection IDs for the given list of task IDs. Partitions the
   * list of task IDs automatically.
   *
   * @param taskIds list of task IDs
   * @return the list of task action IDs
   */
  fun getIdsByTaskIdsPartitioned(taskIds: List<Long>): List<Long>

  /**
   * Deletes objects of element collection table partitioned.
   *
   * @param constraintSelectionIds list of IDs of task constraint selections to delete
   */
  fun deleteConstraintElementsPartitioned(constraintSelectionIds: List<Long>)

  /**
   * Deletes objects in partitioned.
   *
   * @param ids list of IDs to delete
   */
  fun deletePartitioned(ids: List<Long>)
}
