/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.shared.repository

interface TopicRepositoryExtension {

  /**
   * Returns a list of topic IDs for the given list of task IDs. Partitions the list of task IDs
   * automatically.
   *
   * @param taskIds list of task IDs
   * @return the list of topic IDs
   */
  fun getIdsByTaskIdsPartitioned(taskIds: List<Long>): List<Long>

  /**
   * Deletes objects partitioned.
   *
   * @param ids list of IDs to delete
   */
  fun deletePartitioned(ids: List<Long>)

  /**
   * Marks a topic as deleted without sending an event.
   *
   * @param topicId the topic id
   */
  fun markAsDeleted(topicId: Long)
}
