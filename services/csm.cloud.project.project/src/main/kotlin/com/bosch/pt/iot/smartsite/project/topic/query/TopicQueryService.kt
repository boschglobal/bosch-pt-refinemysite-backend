/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.query

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TOPIC_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicDto
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicWithMessageCountDto
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import datadog.trace.api.Trace
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TopicQueryService(
    private val topicRepository: TopicRepository,
    private val taskRepository: TaskRepository
) {

  @Trace
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#identifier)")
  @Transactional(readOnly = true)
  fun findTopicByIdentifier(identifier: TopicId): TopicWithMessageCountDto {

    val topic =
        topicRepository.findOneByIdentifier(identifier, TopicDto::class.java)
            ?: throw AggregateNotFoundException(
                TOPIC_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

    return mapTopicWithMessageCount(setOf(topic))[0]
  }

  @Trace
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#identifier)")
  @Transactional(readOnly = true)
  fun findTopicById(identifier: TopicId): Topic =
      topicRepository.findOneByIdentifier(identifier)
          ?: throw AggregateNotFoundException(
              TOPIC_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findProjectIdentifierByIdentifier(
      identifier: TopicId,
  ): ProjectId = topicRepository.findProjectIdentifierByIdentifier(identifier)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  fun findPagedTopicByTaskIdAndTopicDate(
      taskIdentifier: TaskId,
      beforeTopicIdentifier: TopicId?,
      limit: Int?
  ): Slice<TopicWithMessageCountDto> {

    val topics: Slice<TopicDto>
    val searchLimit =
        if (limit == null || limit > TOPIC_DEFAULT_LIMIT) TOPIC_DEFAULT_LIMIT else limit
    val pageable: Pageable = PageRequest.of(0, searchLimit, Sort.by("createdDate").descending())

    taskRepository.findOneByIdentifier(taskIdentifier)
        ?: throw AggregateNotFoundException(
            TASK_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

    topics =
        if (beforeTopicIdentifier == null) {

          // Read the newest amount of topics defined by limit
          topicRepository.findAllByTaskIdentifierInAndDeletedFalse(
              setOf(taskIdentifier), pageable, TopicDto::class.java)
        } else {

          // Read the amount of topics defined by limit that are older than the one with the given
          // before id
          val beforeTopic =
              topicRepository.findOneByIdentifier(beforeTopicIdentifier, TopicDto::class.java)
                  ?: throw AggregateNotFoundException(
                      TOPIC_VALIDATION_ERROR_NOT_FOUND, beforeTopicIdentifier.toString())

          topicRepository
              .findAllByTaskIdentifierAndDeletedFalseAndCreatedDateLessThanOrderByCreatedDateDesc(
                  taskIdentifier, beforeTopic.createdDate, pageable, TopicDto::class.java)
        }

    val topicsWithMessageCount = mapTopicWithMessageCount(topics.content)
    return SliceImpl(topicsWithMessageCount, topics.pageable, topics.hasNext())
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  fun findByTaskIdentifiers(
      taskIdentifiers: MutableCollection<TaskId>,
      pageable: Pageable
  ): Slice<TopicWithMessageCountDto> {

    val sortedPageable: Pageable =
        PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by("taskIdentifier", "createdDate", "identifier").descending())

    val topics =
        topicRepository.findAllByTaskIdentifierInAndDeletedFalse(
            taskIdentifiers, sortedPageable, TopicDto::class.java)
    val topicsWithMessageCount = mapTopicWithMessageCount(topics.content)
    return SliceImpl(topicsWithMessageCount, topics.pageable, topics.hasNext())
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  fun findAllByTaskIdentifiers(taskIdentifiers: List<TaskId>): List<Topic> {
    return topicRepository.findAllByTaskIdentifierIn(taskIdentifiers)
  }

  private fun mapTopicWithMessageCount(
      topics: Collection<TopicDto>
  ): List<TopicWithMessageCountDto> {

    val topicIdentifiers = topics.map(TopicDto::identifier)
    val messageCounts =
        topicRepository
            .findMessageCountByTopicIdentifierIn(topicIdentifiers)
            .associateBy({ it.left }, { it.right })

    return topics.map { topic: TopicDto ->
      TopicWithMessageCountDto(topic, messageCounts[topic.identifier]!!)
    }
  }

  companion object {
    const val TOPIC_DEFAULT_LIMIT = 50
  }
}
