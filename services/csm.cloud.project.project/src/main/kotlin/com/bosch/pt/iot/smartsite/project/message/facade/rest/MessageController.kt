/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.message.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.message.boundary.MessageQueryService
import com.bosch.pt.iot.smartsite.project.message.command.api.CreateMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.api.DeleteMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.handler.CreateMessageCommandHandler
import com.bosch.pt.iot.smartsite.project.message.command.handler.DeleteMessageCommandHandler
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.request.CreateMessageResource
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageBatchResource
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageListResource
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageResource
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory.MessageBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory.MessageListResourceFactory
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory.MessageResourceFactory
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.query.TopicQueryService
import datadog.trace.api.Trace
import jakarta.annotation.Nonnull
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath

@ApiVersion
@RestController
class MessageController(
    private val createMessageCommandHandler: CreateMessageCommandHandler,
    private val deleteMessageCommandHandler: DeleteMessageCommandHandler,
    private val messageQueryService: MessageQueryService,
    private val topicQueryService: TopicQueryService,
    private val messageResourceFactory: MessageResourceFactory,
    private val messageListResourceFactory: MessageListResourceFactory,
    private val messageBatchResourceFactory: MessageBatchResourceFactory
) {

  @Trace
  @PostMapping(*[MESSAGES_BY_TOPIC_ID_ENDPOINT, MESSAGE_BY_TOPIC_ID_AND_MESSAGE_ID_ENDPOINT])
  fun createMessage(
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicId: TopicId,
      @PathVariable(value = PATH_VARIABLE_MESSAGE_ID, required = false) messageId: MessageId?,
      @Nonnull @RequestBody @Valid createMessageResource: CreateMessageResource
  ): ResponseEntity<MessageResource> {
    val topic = topicQueryService.findTopicById(topicId)
    val message = Message(messageId ?: MessageId(), createMessageResource.content, topic)

    val projectIdentifier = topicQueryService.findProjectIdentifierByIdentifier(topicId)

    val messageIdentifier =
        createMessageCommandHandler.handle(
            CreateMessageCommand(
                identifier = messageId ?: MessageId(),
                content = message.content,
                topicIdentifier = topicId,
                projectIdentifier = projectIdentifier))

    val messageCreated = messageQueryService.findOneByIdentifier(messageIdentifier)

    val location =
        fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(MESSAGE_BY_TOPIC_ID_AND_MESSAGE_ID_ENDPOINT)
            .buildAndExpand(topicId, messageCreated.identifier)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(messageCreated.version.toString())
        .body(messageResourceFactory.build(messageCreated))
  }

  @Trace
  @GetMapping(MESSAGES_BY_TOPIC_ID_ENDPOINT)
  fun findAllMessages(
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicId: TopicId,
      @RequestParam(name = "before", required = false) before: MessageId?,
      @RequestParam(name = "limit", required = false) limit: Int?
  ): ResponseEntity<MessageListResource> {
    val messages: Slice<MessageDto> =
        messageQueryService.findPagedMessageByTopicIdAndMessageDate(topicId, before, limit)
    return ResponseEntity.ok().body(messageListResourceFactory.build(messages, topicId, limit))
  }

  @PostMapping(MESSAGES_BATCH_ENDPOINT)
  fun findByTaskIdentifiers(
      @Nonnull @RequestBody @Valid batchRequestResource: BatchRequestResource,
      @PageableDefault(size = 100) pageable: Pageable,
      @RequestParam(name = "identifierType", defaultValue = BatchRequestIdentifierType.MESSAGE)
      identifierType: String
  ): ResponseEntity<MessageBatchResource> {

    return if (identifierType == TASK) {
      val taskIdentifiers: Set<TaskId> = batchRequestResource.ids.asTaskIds()
      val messages: Slice<MessageDto> =
          messageQueryService.findByTaskIdentifiers(taskIdentifiers, pageable)
      ResponseEntity.ok().body(messageBatchResourceFactory.build(messages))
    } else {
      throw BatchIdentifierTypeNotSupportedException(
          COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
    }
  }

  @Trace
  @GetMapping(MESSAGE_BY_MESSAGE_ID_ENDPOINT)
  fun findMessage(
      @PathVariable(PATH_VARIABLE_MESSAGE_ID) messageId: MessageId
  ): ResponseEntity<MessageResource> {
    val message = messageQueryService.findOneByIdentifier(messageId)
    return ResponseEntity.ok()
        .eTag(message.version.toString())
        .body(messageResourceFactory.build(message))
  }

  @Trace
  @DeleteMapping(MESSAGE_BY_MESSAGE_ID_ENDPOINT)
  fun deleteMessage(
      @PathVariable(PATH_VARIABLE_MESSAGE_ID) messageId: MessageId
  ): ResponseEntity<Void> {
    deleteMessageCommandHandler.handle(DeleteMessageCommand(messageId))
    return ResponseEntity.noContent().build()
  }

  companion object {
    const val PATH_VARIABLE_MESSAGE_ID = "messageId"
    const val PATH_VARIABLE_TOPIC_ID = "topicId"

    const val MESSAGE_BY_MESSAGE_ID_ENDPOINT = "/projects/tasks/topics/messages/{messageId}"
    const val MESSAGES_BY_TOPIC_ID_ENDPOINT = "/projects/tasks/topics/{topicId}/messages"
    const val MESSAGE_BY_TOPIC_ID_AND_MESSAGE_ID_ENDPOINT =
        "/projects/tasks/topics/{topicId}/messages/{messageId}"

    const val MESSAGES_BATCH_ENDPOINT = "/projects/tasks/topics/messages"
  }
}
