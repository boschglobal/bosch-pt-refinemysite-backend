/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.factory.AttachmentResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.TopicAttachmentListResource
import org.springframework.stereotype.Component

@Component
class TopicAttachmentListResourceFactory(
    private val attachmentResourceFactoryHelper: AttachmentResourceFactoryHelper
) {
  fun build(attachments: Collection<AttachmentDto>): TopicAttachmentListResource {
    if (attachments.isEmpty()) {
      return TopicAttachmentListResource(emptyList())
    }
    val attachmentResources = attachmentResourceFactoryHelper.build(attachments)
    return TopicAttachmentListResource(attachmentResources)
  }
}
