/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource
import com.bosch.pt.iot.smartsite.user.model.User
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class TaskAttachmentResourceFactoryHelper(
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  fun build(attachment: AttachmentDto, allowedToDelete: Boolean): TaskAttachmentResource {

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
    return TaskAttachmentResource(
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
            attachment.taskIdentifier)
        .apply { this.addLinks(allowedToDelete) }
  }

  private fun TaskAttachmentResource.addLinks(allowedToDelete: Boolean) {
    // delete link
    addIf(allowedToDelete) {
      linkFactory
          .linkTo(TaskAttachmentController.ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
          .withParameters(mapOf(TaskAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
          .withRel(TaskAttachmentResource.LINK_DELETE)
    }

    // preview link
    add(
        linkFactory
            .linkTo(TaskAttachmentController.DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(TaskAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(TaskAttachmentResource.LINK_PREVIEW))

    // data link
    add(
        linkFactory
            .linkTo(TaskAttachmentController.DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(TaskAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(TaskAttachmentResource.LINK_DATA))

    // original link
    add(
        linkFactory
            .linkTo(TaskAttachmentController.DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT)
            .withParameters(
                mapOf(TaskAttachmentController.PATH_VARIABLE_ATTACHMENT_ID to identifier))
            .withRel(TaskAttachmentResource.LINK_ORIGINAL))
  }
}
