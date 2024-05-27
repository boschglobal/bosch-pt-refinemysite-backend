/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentBatchResource
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource

class TaskAttachmentBatchResource(
    attachments: Collection<AttachmentResource>,
    pageNumber: Int,
    pageSize: Int
) : AttachmentBatchResource(attachments, pageNumber, pageSize)
