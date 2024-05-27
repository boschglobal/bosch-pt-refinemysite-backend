/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentListResource

class MessageAttachmentListResource(attachments: Collection<MessageAttachmentResource>) :
    AttachmentListResource(attachments)
