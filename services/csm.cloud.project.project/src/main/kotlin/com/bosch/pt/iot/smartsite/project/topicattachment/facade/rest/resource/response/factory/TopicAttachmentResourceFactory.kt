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
package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import org.springframework.stereotype.Component

@Component
class TopicAttachmentResourceFactory(
    private val topicAttachmentResourceFactoryHelper: TopicAttachmentResourceFactoryHelper
) {

  fun build(attachment: AttachmentDto) = topicAttachmentResourceFactoryHelper.build(attachment)
}
