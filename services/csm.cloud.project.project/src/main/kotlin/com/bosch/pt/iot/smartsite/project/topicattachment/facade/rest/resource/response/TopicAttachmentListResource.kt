/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentListResource
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource

class TopicAttachmentListResource(attachmentResources: Collection<AttachmentResource>) :
    AttachmentListResource(attachmentResources)
