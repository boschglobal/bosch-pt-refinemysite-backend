/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.shared.repository

interface MessageRepositoryExtension {

  /**
   * Returns a list of message IDs for the given list of topic IDs. Partitions the list of topic IDs
   * automatically.
   *
   * @param topicIds list of topic IDs
   * @return the list of message IDs
   */
  fun getIdsByTopicIdsPartitioned(topicIds: List<Long>): List<Long>

  /**
   * Deletes objects in partitioned.
   *
   * @param ids list of IDs to delete
   */
  fun deletePartitioned(ids: List<Long>)
}
