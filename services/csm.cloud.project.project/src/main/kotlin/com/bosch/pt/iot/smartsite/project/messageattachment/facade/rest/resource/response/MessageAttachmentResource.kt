/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.util.Date
import java.util.Objects
import java.util.UUID

class MessageAttachmentResource(
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
    val taskId: TaskId,
    val topicId: TopicId,
    val messageId: MessageId
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

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is MessageAttachmentResource) {
      return false
    }
    if (!super.equals(other)) {
      return false
    }
    return (taskId == other.taskId && topicId == other.topicId && messageId == other.messageId)
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    return Objects.hash(super.hashCode(), taskId, topicId, messageId)
  }

  companion object {
    const val LINK_PREVIEW = "preview"
    const val LINK_DATA = "data"
    const val LINK_ORIGINAL = "original"
  }
}
