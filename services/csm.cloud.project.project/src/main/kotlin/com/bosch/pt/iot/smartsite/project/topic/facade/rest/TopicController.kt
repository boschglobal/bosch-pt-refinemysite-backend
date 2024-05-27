/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import com.bosch.pt.iot.smartsite.project.topic.boundary.TopicRequestDeleteService
import com.bosch.pt.iot.smartsite.project.topic.command.api.CreateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.api.DeescalateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.api.EscalateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.handler.CreateTopicCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.command.handler.DeescalateTopicCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.command.handler.EscalateTopicCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.request.CreateTopicResource
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicBatchResource
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicListResource
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicResource
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory.TopicBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory.TopicListResourceFactory
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory.TopicResourceFactory
import com.bosch.pt.iot.smartsite.project.topic.query.TopicQueryService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@RestController
class TopicController(
    private val topicResourceFactory: TopicResourceFactory,
    private val topicBatchResourceFactory: TopicBatchResourceFactory,
    private val topicListResourceFactory: TopicListResourceFactory,
    private val createTopicCommandHandler: CreateTopicCommandHandler,
    private val escalateTopicCommandHandler: EscalateTopicCommandHandler,
    private val deescalateTopicCommandHandler: DeescalateTopicCommandHandler,
    private val topicQueryService: TopicQueryService,
    private val taskQueryService: TaskQueryService,
    private val topicRequestDeleteService: TopicRequestDeleteService
) {

  @PostMapping(*[TOPICS_BY_TASK_ID_ENDPOINT, TOPIC_BY_TASK_ID_AND_TOPIC_ID_ENDPOINT])
  fun createTopic(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskId: TaskId,
      @PathVariable(value = PATH_VARIABLE_TOPIC_ID, required = false) topicId: TopicId?,
      @RequestBody @Valid createTopicResource: CreateTopicResource
  ): ResponseEntity<TopicResource> {
    val projectIdentifier = taskQueryService.findProjectIdentifierByIdentifier(taskId)

    val topicIdentifier =
        createTopicCommandHandler.handle(
            CreateTopicCommand(
                topicId ?: TopicId(),
                createTopicResource.criticality,
                createTopicResource.description,
                taskId,
                projectIdentifier))

    val topicCreated = topicQueryService.findTopicByIdentifier(topicIdentifier)

    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(TOPIC_BY_TASK_ID_AND_TOPIC_ID_ENDPOINT)
            .buildAndExpand(taskId, topicCreated.identifier)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(topicCreated.version.toString())
        .body(topicResourceFactory.build(topicCreated))
  }

  /**
   * Retrieving a list of all topics for a given task.
   *
   * @param taskId task id
   * @param before the id of the last topic received before (aka. the cursor for the cursor-based
   *   paging)
   * @param limit the number of topics to be read (aka. the offset for the cursor-based paging)
   * @return TopicListResource containing list of topics resources
   */
  @GetMapping(TOPICS_BY_TASK_ID_ENDPOINT)
  fun findAllTopics(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskId: TaskId,
      @RequestParam(name = "before", required = false) before: TopicId?,
      @RequestParam(name = "limit", required = false) limit: Int?
  ): ResponseEntity<TopicListResource> {
    val topics = topicQueryService.findPagedTopicByTaskIdAndTopicDate(taskId, before, limit)
    return ResponseEntity.ok().body(topicListResourceFactory.build(topics, taskId, limit))
  }

  @PostMapping(TOPICS_ENDPOINT)
  fun findByTaskIdentifiers(
      @RequestBody @Valid batchRequestResource: BatchRequestResource,
      @PageableDefault(size = 100) pageable: Pageable,
      @RequestParam(name = "identifierType", defaultValue = TASK) identifierType: String
  ): ResponseEntity<TopicBatchResource> =
      if (identifierType == TASK) {
        val taskIdentifiers = batchRequestResource.ids.asTaskIds()
        val topics =
            topicQueryService.findByTaskIdentifiers(taskIdentifiers.toMutableSet(), pageable)
        ResponseEntity.ok().body(topicBatchResourceFactory.build(topics))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @GetMapping(TOPIC_BY_TOPIC_ID_ENDPOINT)
  fun findTopic(
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicId: TopicId
  ): ResponseEntity<TopicResource> {
    val topic = topicQueryService.findTopicByIdentifier(topicId)
    return ResponseEntity.ok()
        .eTag(topic.version.toString())
        .body(topicResourceFactory.build(topic))
  }

  @PostMapping(ESCALATE_TOPIC_BY_TOPIC_ID_ENDPOINT)
  fun escalateTopic(
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicId: TopicId
  ): ResponseEntity<TopicResource> {

    escalateTopicCommandHandler.handle(EscalateTopicCommand(topicId))

    val topic = topicQueryService.findTopicByIdentifier(topicId)
    return ResponseEntity.ok()
        .eTag(topic.version.toString())
        .body(topicResourceFactory.build(topic))
  }

  @PostMapping(DEESCALATE_TOPIC_BY_TOPIC_ID_ENDPOINT)
  fun deEscalateTopic(
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicId: TopicId
  ): ResponseEntity<TopicResource> {
    deescalateTopicCommandHandler.handle(DeescalateTopicCommand(topicId))
    val topic = topicQueryService.findTopicByIdentifier(topicId)
    return ResponseEntity.ok()
        .eTag(topic.version.toString())
        .body(topicResourceFactory.build(topic))
  }

  @DeleteMapping(TOPIC_BY_TOPIC_ID_ENDPOINT)
  fun deleteTopic(@PathVariable(PATH_VARIABLE_TOPIC_ID) topicId: TopicId): ResponseEntity<Void> {
    topicRequestDeleteService.markAsDeletedAndSendEvent(topicId)
    return ResponseEntity.noContent().build()
  }

  companion object {
    const val PATH_VARIABLE_TASK_ID = "taskId"
    const val PATH_VARIABLE_TOPIC_ID = "topicId"

    const val TOPICS_ENDPOINT = "/projects/tasks/topics"
    const val TOPICS_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/topics"
    const val TOPIC_BY_TASK_ID_AND_TOPIC_ID_ENDPOINT = "/projects/tasks/{taskId}/topics/{topicId}"
    const val TOPIC_BY_TOPIC_ID_ENDPOINT = "/projects/tasks/topics/{topicId}"
    const val DEESCALATE_TOPIC_BY_TOPIC_ID_ENDPOINT = "/projects/tasks/topics/{topicId}/deescalate"
    const val ESCALATE_TOPIC_BY_TOPIC_ID_ENDPOINT = "/projects/tasks/topics/{topicId}/escalate"
  }
}
