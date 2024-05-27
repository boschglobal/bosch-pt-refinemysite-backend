/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.TopicAttachmentResource
import com.bosch.pt.iot.smartsite.user.model.User
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class TopicAttachmentResourceFactoryHelper(
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  fun build(attachment: AttachmentDto): TopicAttachmentResource {
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
    return TopicAttachmentResource(
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
            attachment.topicIdentifier!!)
        .apply { addLinks() }
  }

  private fun TopicAttachmentResource.addLinks() {
    addPreviewLink()
    addDataLink()
    addOriginalLink()
  }

  private fun TopicAttachmentResource.addPreviewLink() {
    this.add(
        linkFactory
            .linkTo(TopicAttachmentController.DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(TopicAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(TopicAttachmentResource.LINK_PREVIEW))
  }

  private fun TopicAttachmentResource.addDataLink() {
    this.add(
        linkFactory
            .linkTo(TopicAttachmentController.DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(TopicAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(TopicAttachmentResource.LINK_DATA))
  }

  private fun TopicAttachmentResource.addOriginalLink() {
    this.add(
        linkFactory
            .linkTo(TopicAttachmentController.DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(TopicAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(TopicAttachmentResource.LINK_ORIGINAL))
  }
}
