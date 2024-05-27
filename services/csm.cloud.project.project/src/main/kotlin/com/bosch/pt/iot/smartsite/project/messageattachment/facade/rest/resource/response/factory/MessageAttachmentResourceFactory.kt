/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import org.springframework.stereotype.Component

@Component
open class MessageAttachmentResourceFactory(
    private val messageAttachmentResourceFactoryHelper: MessageAttachmentResourceFactoryHelper
) {
  open fun build(attachment: AttachmentDto) =
      messageAttachmentResourceFactoryHelper.build(attachment)
}
