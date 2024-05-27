/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.MESSAGE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import datadog.trace.api.Trace
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MessageQueryService(private val messageRepository: MessageRepository) {

  @Trace
  @PreAuthorize("@messageAuthorizationComponent.hasViewPermissionOnMessage(#messageIdentifier)")
  @Transactional(readOnly = true)
  fun findOneByIdentifier(messageIdentifier: MessageId) =
      messageRepository.findOneByIdentifier(messageIdentifier, MessageDto::class.java)
          ?: throw AggregateNotFoundException(
              MESSAGE_VALIDATION_ERROR_NOT_FOUND, messageIdentifier.toString())

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  fun findByTaskIdentifiers(
      taskIdentifiers: Collection<TaskId>,
      pageable: Pageable
  ): Slice<MessageDto> {
    val sortedPageable: Pageable =
        PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by("topicIdentifier", "identifier").descending())
    return messageRepository.findByTopicTaskIdentifierIn(
        taskIdentifiers, sortedPageable, MessageDto::class.java)
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findProjectIdentifierByIdentifier(
      messageId: MessageId,
  ): ProjectId {
    return messageRepository.findProjectIdentifierByIdentifier(messageId)
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  fun findAllMessagesByTaskIdentifiers(taskIdentifiers: Collection<TaskId>): List<Message> {
    return messageRepository.findAllMessagesByTaskIdentifiers(taskIdentifiers.toSet())
  }

  @Trace
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#topicIdentifier)")
  @Transactional(readOnly = true)
  fun findPagedMessageByTopicIdAndMessageDate(
      topicIdentifier: TopicId,
      messageIdentifier: MessageId?,
      limit: Int?
  ): Slice<MessageDto> {
    val searchLimit =
        if (limit == null || limit > MESSAGE_DEFAULT_LIMIT) MESSAGE_DEFAULT_LIMIT else limit
    val pageSize = PageRequest.of(0, searchLimit)

    return if (messageIdentifier == null) {
      // Read the newest amount of messages defined by limit
      messageRepository.findAllByTopicIdentifierOrderByCreatedDateDesc(
          topicIdentifier, pageSize, MessageDto::class.java)
    } else {

      // Read the amount of messages defined by limit that are older than the message with given
      // before id
      val lastMessage =
          messageRepository.findOneByIdentifier(messageIdentifier, MessageDto::class.java)
              ?: throw AggregateNotFoundException(
                  MESSAGE_VALIDATION_ERROR_NOT_FOUND, messageIdentifier.toString())

      messageRepository.findAllByTopicIdentifierAndCreatedDateLessThanOrderByCreatedDateDesc(
          topicIdentifier, lastMessage.createdDate, pageSize, MessageDto::class.java)
    }
  }

  companion object {
    const val MESSAGE_DEFAULT_LIMIT = 50
  }
}
