/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.topic.authorization.TopicAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.TopicController
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicResource
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicResource.Companion.EMBEDDED_TOPIC_ATTACHMENTS
import com.bosch.pt.iot.smartsite.project.topic.query.TopicQueryService
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicWithMessageCountDto
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory.TopicAttachmentListResourceFactory
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder.Companion.buildWithFallback
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class TopicResourceFactoryHelper(
    messageSource: MessageSource,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val topicAuthorizationComponent: TopicAuthorizationComponent,
    private val topicAttachmentQueryService: TopicAttachmentQueryService,
    private val participantQueryService: ParticipantQueryService,
    private val topicQueryService: TopicQueryService,
    private val topicAttachmentListResourceFactory: TopicAttachmentListResourceFactory,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService,
) : AbstractResourceFactoryHelper(messageSource) {

  fun build(
      topicWithMessageCountDtos: List<TopicWithMessageCountDto>,
      addEmbedded: Boolean
  ): List<TopicResource> {

    if (topicWithMessageCountDtos.isEmpty()) {
      return emptyList()
    }

    val topics =
        topicQueryService.findAllByTaskIdentifiers(
            topicWithMessageCountDtos.map { it.taskIdentifier })

    val auditUsers = userService.findAuditUsers(topics)

    val topicIdentifiers =
        topicWithMessageCountDtos.map(TopicWithMessageCountDto::identifier).toSet()
    val deletePermissions =
        topicAuthorizationComponent.filterTopicsWithDeletePermission(topicIdentifiers)
    val tasksWithViewPermission =
        taskAuthorizationComponent.filterTasksWithViewPermission(
            topicWithMessageCountDtos.map(TopicWithMessageCountDto::taskIdentifier).toSet())
    val attachmentsByTopicIdentifier: Map<TopicId, List<AttachmentDto>> =
        if (addEmbedded)
            topicAttachmentQueryService.findAllAndMappedByTopicIdentifierIn(topicIdentifiers)
        else emptyMap()

    val creatorParticipants =
        participantQueryService.findActiveAndInactiveParticipants(
            topics.first().task.project.identifier,
            collectUserIdsOfTopicsCreators(topicWithMessageCountDtos))

    return topicWithMessageCountDtos.map { topic: TopicWithMessageCountDto ->
      build(
          topic = topic,
          creatorParticipant = creatorParticipants[topic.createdByIdentifier]!!,
          attachments = attachmentsByTopicIdentifier[topic.identifier] ?: emptyList(),
          createdBy = auditUsers[topic.createdByIdentifier]!!,
          lastModifiedBy = auditUsers[topic.lastModifiedByIdentifier]!!,
          hasDeletePermission = deletePermissions.contains(topic.identifier),
          hasTaskViewPermission = tasksWithViewPermission.contains(topic.taskIdentifier),
          addEmbedded = addEmbedded)
    }
  }

  private fun build(
      topic: TopicWithMessageCountDto,
      attachments: Collection<AttachmentDto>,
      creatorParticipant: Participant,
      createdBy: User,
      lastModifiedBy: User,
      hasDeletePermission: Boolean,
      hasTaskViewPermission: Boolean,
      addEmbedded: Boolean
  ): TopicResource {
    return TopicResource(
            id = topic.identifier.toUuid(),
            version = topic.version,
            createdDate = topic.createdDate,
            createdBy = referTo(createdBy, deletedUserReference)!!,
            lastModifiedDate = topic.lastModifiedDate,
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            taskId = topic.taskIdentifier,
            criticality = topic.criticality,
            description = topic.description,
            messages = topic.messageCount,
            creatorPicture =
                buildWithFallback(
                    creatorParticipant.user?.profilePicture?.identifier,
                    topic.createdByIdentifier.identifier))
        .apply {
          addLinks(topic.identifier, hasDeletePermission, hasTaskViewPermission)
          addResourceSupplier(EMBEDDED_TOPIC_ATTACHMENTS) {
            topicAttachmentListResourceFactory.build(attachments)
          }
          if (addEmbedded) {
            embed(EMBEDDED_TOPIC_ATTACHMENTS)
          }
        }
  }

  private fun TopicResource.addLinks(
      identifier: TopicId,
      allowedToDelete: Boolean,
      allowedToViewTask: Boolean
  ) {
    addDeleteLink(identifier, allowedToDelete)
    addGetMessagesLink(identifier, allowedToViewTask)
    addCreateMessageLink(identifier, allowedToViewTask)
    addEscalateLink(identifier, allowedToViewTask)
    addDeEscalateLink(identifier, allowedToViewTask)
  }

  fun TopicResource.addDeleteLink(identifier: TopicId, allowedToDelete: Boolean) {
    addIf(allowedToDelete) {
      linkFactory
          .linkTo(TopicController.TOPIC_BY_TOPIC_ID_ENDPOINT)
          .withParameters(mapOf(TopicController.PATH_VARIABLE_TOPIC_ID to identifier))
          .withRel(TopicResource.LINK_DELETE)
    }
  }

  private fun TopicResource.addGetMessagesLink(identifier: TopicId, allowedToViewTask: Boolean) {
    addIf(allowedToViewTask) {
      linkFactory
          .linkTo(MessageController.MESSAGES_BY_TOPIC_ID_ENDPOINT)
          .withParameters(mapOf(MessageController.PATH_VARIABLE_TOPIC_ID to identifier))
          .withRel(TopicResource.LINK_MESSAGE)
    }
  }

  private fun TopicResource.addCreateMessageLink(identifier: TopicId, allowedToViewTask: Boolean) {
    addIf(allowedToViewTask) {
      linkFactory
          .linkTo(MessageController.MESSAGES_BY_TOPIC_ID_ENDPOINT)
          .withParameters(mapOf(MessageController.PATH_VARIABLE_TOPIC_ID to identifier))
          .withRel(TopicResource.LINK_CREATE_MESSAGE)
    }
  }

  private fun TopicResource.addEscalateLink(identifier: TopicId, allowedToViewTask: Boolean) {
    addIf(allowedToViewTask && criticality === TopicCriticalityEnum.UNCRITICAL) {
      linkFactory
          .linkTo(TopicController.ESCALATE_TOPIC_BY_TOPIC_ID_ENDPOINT)
          .withParameters(mapOf(TopicController.PATH_VARIABLE_TOPIC_ID to identifier))
          .withRel(TopicResource.LINK_ESCALATE)
    }
  }

  private fun TopicResource.addDeEscalateLink(identifier: TopicId, allowedToViewTask: Boolean) {
    addIf(allowedToViewTask && criticality === TopicCriticalityEnum.CRITICAL) {
      linkFactory
          .linkTo(TopicController.DEESCALATE_TOPIC_BY_TOPIC_ID_ENDPOINT)
          .withParameters(mapOf(TopicController.PATH_VARIABLE_TOPIC_ID to identifier))
          .withRel(TopicResource.LINK_DEESCALATE)
    }
  }

  private fun collectUserIdsOfTopicsCreators(topics: List<TopicWithMessageCountDto>) =
      topics.map { it.createdByIdentifier }.toSet()
}
