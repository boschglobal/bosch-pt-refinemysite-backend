/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.MessageAttachmentListResource
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.MessageAttachmentResource
import org.springframework.stereotype.Component

@Component
class MessageAttachmentListResourceFactory(
    private val messageAttachmentResourceFactoryHelper: MessageAttachmentResourceFactoryHelper
) {

  fun build(attachments: Collection<AttachmentDto>): MessageAttachmentListResource {
    if (attachments.isEmpty()) {
      return MessageAttachmentListResource(emptyList())
    }
    val messageAttachmentResources: Collection<MessageAttachmentResource> =
        attachments.map { attachment: AttachmentDto ->
          messageAttachmentResourceFactoryHelper.build(attachment)
        }
    return MessageAttachmentListResource(messageAttachmentResources)
  }
}
