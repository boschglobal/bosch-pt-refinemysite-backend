/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.MessageAttachmentResource
import com.bosch.pt.iot.smartsite.user.model.User
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class MessageAttachmentResourceFactoryHelper(
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  fun build(attachment: AttachmentDto): MessageAttachmentResource {
    // Create user resource references
    val createdBy =
        referTo(
            attachment.createdByIdentifier,
            User.getDisplayName(attachment.createdByFirstName, attachment.createdByLastName),
            deletedUserReference,
            attachment.createdByDeleted)
    val lastModifiedBy =
        referTo(
            attachment.lastModifiedByIdentifier,
            User.getDisplayName(
                attachment.lastModifiedByFirstName, attachment.lastModifiedByLastName),
            deletedUserReference,
            attachment.lastModifiedByDeleted)
    val resource =
        MessageAttachmentResource(
            attachment.identifier,
            attachment.version,
            attachment.createdDate,
            attachment.lastModifiedDate,
            createdBy,
            lastModifiedBy,
            attachment.captureDate,
            attachment.fileName,
            attachment.fileSize,
            attachment.imageHeight,
            attachment.imageWidth,
            attachment.taskIdentifier,
            attachment.topicIdentifier!!,
            attachment.messageIdentifier!!)
    resource.addLinks()
    return resource
  }

  private fun MessageAttachmentResource.addLinks() {
    addPreviewLink()
    addDataLink()
    addOriginalLink()
  }

  private fun MessageAttachmentResource.addPreviewLink() {
    this.add(
        linkFactory
            .linkTo(MessageAttachmentController.PREVIEW_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(MessageAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(MessageAttachmentResource.LINK_PREVIEW))
  }

  private fun MessageAttachmentResource.addDataLink() {
    this.add(
        linkFactory
            .linkTo(MessageAttachmentController.FULL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(MessageAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(MessageAttachmentResource.LINK_DATA))
  }

  private fun MessageAttachmentResource.addOriginalLink() {
    this.add(
        linkFactory
            .linkTo(MessageAttachmentController.ORIGINAL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(MessageAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(MessageAttachmentResource.LINK_ORIGINAL))
  }
}
