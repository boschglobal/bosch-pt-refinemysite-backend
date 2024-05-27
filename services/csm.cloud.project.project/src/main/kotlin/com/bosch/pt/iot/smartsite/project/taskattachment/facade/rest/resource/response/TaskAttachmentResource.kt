/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import java.util.Date
import java.util.UUID

class TaskAttachmentResource(
    identifier: UUID,
    version: Long,
    createdDate: Date,
    lastModifiedDate: Date,
    createdBy: ResourceReference,
    lastModifiedBy: ResourceReference,
    captureDate: Date?,
    fileName: String,
    fileSize: Long,
    imageHeight: Long?,
    imageWidth: Long?,
    val taskId: TaskId
) :
    AttachmentResource(
        identifier,
        version,
        createdDate,
        lastModifiedDate,
        createdBy,
        lastModifiedBy,
        captureDate,
        fileName,
        fileSize,
        imageHeight,
        imageWidth) {

  companion object {
    const val LINK_PREVIEW = "preview"
    const val LINK_DATA = "data"
    const val LINK_ORIGINAL = "original"
    const val LINK_DELETE = "delete"
  }
}
