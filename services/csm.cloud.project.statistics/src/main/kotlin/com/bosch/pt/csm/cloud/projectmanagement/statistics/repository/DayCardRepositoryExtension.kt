/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

interface DayCardRepositoryExtension {

  /**
   * Deletes objects with the given IDs.
   *
   * @param ids list of IDs to delete
   */
  fun deleteAll(ids: List<Long>)
}
