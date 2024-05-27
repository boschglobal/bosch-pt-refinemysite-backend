/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentListResource
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource

class TaskAttachmentListResource(attachmentResources: Collection<AttachmentResource>) :
    AttachmentListResource(attachmentResources)
