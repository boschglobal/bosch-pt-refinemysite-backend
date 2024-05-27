/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.service

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.TopicRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TopicService(private val topicRepository: TopicRepository) {

  @Trace fun save(topic: Topic) = topicRepository.save(topic)

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      topicRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      topicRepository.deleteTopicAndAllRelatedDocuments(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      topicRepository.deleteByVersion(identifier, version, projectIdentifier)
}
