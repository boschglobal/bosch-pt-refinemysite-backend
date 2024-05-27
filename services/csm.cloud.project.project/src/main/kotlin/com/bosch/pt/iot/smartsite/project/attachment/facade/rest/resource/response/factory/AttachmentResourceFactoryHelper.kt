/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory.MessageAttachmentResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory.TopicAttachmentResourceFactoryHelper
import org.springframework.stereotype.Component

@Component
class AttachmentResourceFactoryHelper(
    private val topicAttachmentResourceFactoryHelper: TopicAttachmentResourceFactoryHelper,
    private val taskAttachmentResourceFactoryHelper: TaskAttachmentResourceFactoryHelper,
    private val messageAttachmentResourceFactoryHelper: MessageAttachmentResourceFactoryHelper
) {

  @JvmOverloads
  fun build(
      attachments: Collection<AttachmentDto>,
      tasksWithDeletePermission: Collection<TaskId> = emptyList()
  ): Collection<AttachmentResource> =
      if (attachments.isEmpty()) {
        emptyList()
      } else
          attachments.map { attachment ->
            if (attachment.messageIdentifier != null) {
              messageAttachmentResourceFactoryHelper.build(attachment)
            } else if (attachment.topicIdentifier != null) {
              topicAttachmentResourceFactoryHelper.build(attachment)
            } else {
              taskAttachmentResourceFactoryHelper.build(
                  attachment, tasksWithDeletePermission.contains(attachment.taskIdentifier))
            }
          }
}
