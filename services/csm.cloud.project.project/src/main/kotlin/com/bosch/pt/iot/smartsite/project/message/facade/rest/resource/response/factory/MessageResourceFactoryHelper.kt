/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.message.authorization.MessageAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.message.boundary.MessageQueryService
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageResource
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageResource.Companion.EMBEDDED_MESSAGE_ATTACHMENTS
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory.MessageAttachmentListResourceFactory
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder.Companion.buildWithFallback
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.stream.Collectors
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class MessageResourceFactoryHelper(
    messageSource: MessageSource,
    private val messageAuthorizationComponent: MessageAuthorizationComponent,
    private val messageAttachmentQueryService: MessageAttachmentQueryService,
    private val participantQueryService: ParticipantQueryService,
    private val messageQueryService: MessageQueryService,
    private val messageAttachmentListResourceFactory: MessageAttachmentListResourceFactory,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService
) : AbstractResourceFactoryHelper(messageSource) {

  fun build(messages: Collection<MessageDto>, addEmbedded: Boolean): List<MessageResource> {
    if (messages.isEmpty()) {
      return emptyList()
    }

    val auditUsers = userService.findAuditUsersForMessageDtos(messages)
    val projectIdentifier =
        messageQueryService.findProjectIdentifierByIdentifier(messages.first().identifier)

    val messageIdentifiers =
        messages.stream().map(MessageDto::identifier).collect(Collectors.toSet())
    val creatorParticipants =
        participantQueryService.findActiveAndInactiveParticipants(
            projectIdentifier = projectIdentifier, collectUserIdsOfMessagesCreators(messages))
    val deletePermissions =
        messageAuthorizationComponent.filterMessagesWithDeletePermission(messageIdentifiers)
    val attachmentsByMessageIdentifier =
        if (addEmbedded)
            messageAttachmentQueryService.findAllAndMappedByMessageIdentifierIn(messageIdentifiers)
        else emptyMap()
    return messages.map { message: MessageDto ->
      build(
          message,
          attachmentsByMessageIdentifier[message.identifier] ?: emptyList(),
          auditUsers[message.createdBy]!!,
          auditUsers[message.lastModifiedBy]!!,
          creatorParticipants[message.createdBy]!!,
          deletePermissions.contains(message.identifier),
          addEmbedded)
    }
  }

  private fun build(
      message: MessageDto,
      attachments: Collection<AttachmentDto>,
      createdBy: User,
      lastModifiedBy: User,
      creatorParticipant: Participant,
      deletePermission: Boolean,
      addEmbedded: Boolean
  ): MessageResource {
    return MessageResource(
            id = message.identifier.toUuid(),
            version = message.version,
            createdDate = message.createdDate,
            createdBy = User.referTo(createdBy, deletedUserReference)!!,
            lastModifiedDate = message.lastModifiedDate,
            lastModifiedBy = User.referTo(lastModifiedBy, deletedUserReference)!!,
            topicId = message.topicIdentifier,
            content = message.content,
            creatorPicture =
                buildWithFallback(
                    creatorParticipant.user?.profilePicture?.identifier,
                    message.createdBy.identifier))
        .apply {
          addDeleteLink(message.identifier, deletePermission)
          addResourceSupplier(EMBEDDED_MESSAGE_ATTACHMENTS) {
            messageAttachmentListResourceFactory.build(attachments)
          }
          if (addEmbedded) {
            embed(EMBEDDED_MESSAGE_ATTACHMENTS)
          }
        }
  }

  private fun MessageResource.addDeleteLink(identifier: MessageId, allowedToDelete: Boolean) {
    addIf(allowedToDelete) {
      linkFactory
          .linkTo(MessageController.MESSAGE_BY_MESSAGE_ID_ENDPOINT)
          .withParameters(mapOf(MessageController.PATH_VARIABLE_MESSAGE_ID to identifier))
          .withRel(MessageResource.LINK_DELETE)
    }
  }

  private fun collectUserIdsOfMessagesCreators(topics: Collection<MessageDto>) =
      topics.map { it.createdBy }.toSet()
}
