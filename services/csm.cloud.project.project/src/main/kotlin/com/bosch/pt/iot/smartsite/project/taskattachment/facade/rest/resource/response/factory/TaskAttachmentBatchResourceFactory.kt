/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.factory.AttachmentResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentBatchResource
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
open class TaskAttachmentBatchResourceFactory(
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val attachmentResourceFactoryHelper: AttachmentResourceFactoryHelper
) {

  @PageLinks
  open fun build(attachments: Slice<AttachmentDto>): TaskAttachmentBatchResource {
    if (attachments.isEmpty) {
      return buildTaskAttachmentBatchResource(emptyList(), attachments.number, attachments.size)
    }

    val taskIdentifiers = attachments.content.map(AttachmentDto::taskIdentifier).toSet()
    val tasksWithDeletePermission =
        taskAuthorizationComponent.filterTasksWithDeletePermission(taskIdentifiers)
    val attachmentResources =
        attachmentResourceFactoryHelper.build(attachments.content, tasksWithDeletePermission)
    return buildTaskAttachmentBatchResource(
        attachmentResources, attachments.number, attachments.size)
  }

  private fun buildTaskAttachmentBatchResource(
      attachments: Collection<AttachmentResource>,
      pageNumber: Int,
      pageSize: Int
  ): TaskAttachmentBatchResource = TaskAttachmentBatchResource(attachments, pageNumber, pageSize)
}
