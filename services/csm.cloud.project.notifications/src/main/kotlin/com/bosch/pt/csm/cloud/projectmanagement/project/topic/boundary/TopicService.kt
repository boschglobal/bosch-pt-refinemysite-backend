/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.TopicRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TopicService(private val topicRepository: TopicRepository) {

  @Trace fun save(topic: Topic): Topic = topicRepository.save(topic)

  @Trace
  fun findLatest(topicIdentifier: UUID, projectIdentifier: UUID): Topic =
      topicRepository.findLatest(topicIdentifier, projectIdentifier)

  @Trace
  fun deleteTopicAndAllRelatedDocuments(topicIdentifier: UUID, projectIdentifier: UUID) =
      topicRepository.deleteTopicAndAllRelatedDocuments(topicIdentifier, projectIdentifier)
}
