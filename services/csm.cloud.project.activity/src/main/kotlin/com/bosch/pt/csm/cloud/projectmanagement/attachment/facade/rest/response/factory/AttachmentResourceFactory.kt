/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.response.factory

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Attachment
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.AttachmentController
import com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.AttachmentController.Companion.PARAM_ATTACHMENT_ID
import com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.AttachmentController.Companion.PARAM_SIZE
import com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.response.AttachmentResource
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource.ResourceReference
import com.bosch.pt.csm.cloud.projectmanagement.common.service.DisplayNameResolver
import org.springframework.stereotype.Component

@Component
class AttachmentResourceFactory(
    private val displayNameResolver: DisplayNameResolver,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(attachment: Attachment) =
      AttachmentResource(
              identifier = attachment.identifier,
              captureDate = attachment.captureDate?.toDate(),
              fileName = attachment.fileName,
              fileSize = attachment.fileSize,
              imageHeight = attachment.imageHeight,
              imageWidth = attachment.imageWidth,
              topicId = attachment.topicId,
              taskId = attachment.taskId,
              messageId = attachment.messageId,
              createdBy = resolve(attachment.auditingInformation.createdBy),
              createdDate = attachment.auditingInformation.createdDate,
              lastModifiedBy = resolve(attachment.auditingInformation.lastModifiedBy),
              lastModifiedDate = attachment.auditingInformation.lastModifiedDate)
          .apply { addLinks() }

  private fun AttachmentResource.addLinks() {
    // preview link
    add(
        linkFactory
            .linkTo(AttachmentController.ATTACHMENT_BY_ID_ENDPOINT)
            .withParameters(
                mapOf(PARAM_ATTACHMENT_ID to identifier, PARAM_SIZE to ImageResolution.SMALL))
            .withRel(AttachmentResource.LINK_PREVIEW))

    // data link
    add(
        linkFactory
            .linkTo(AttachmentController.ATTACHMENT_BY_ID_ENDPOINT)
            .withParameters(
                mapOf(PARAM_ATTACHMENT_ID to identifier, PARAM_SIZE to ImageResolution.FULL))
            .withRel(AttachmentResource.LINK_DATA))

    // original link
    add(
        linkFactory
            .linkTo(AttachmentController.ATTACHMENT_BY_ID_ENDPOINT)
            .withParameters(
                mapOf(PARAM_ATTACHMENT_ID to identifier, PARAM_SIZE to ImageResolution.ORIGINAL))
            .withRel(AttachmentResource.LINK_ORIGINAL))
  }

  private fun resolve(unresolvedObjectReference: UnresolvedObjectReference) =
      ResourceReference(
          unresolvedObjectReference.identifier,
          displayNameResolver.resolve(unresolvedObjectReference))
}
