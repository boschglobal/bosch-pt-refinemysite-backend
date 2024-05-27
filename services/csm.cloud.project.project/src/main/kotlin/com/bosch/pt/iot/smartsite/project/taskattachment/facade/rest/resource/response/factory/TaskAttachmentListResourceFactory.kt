/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.AttachmentResource
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.factory.AttachmentResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentListResource
import org.springframework.stereotype.Component

@Component
class TaskAttachmentListResourceFactory(
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val attachmentResourceFactoryHelper: AttachmentResourceFactoryHelper
) {

  fun build(
      attachments: Collection<AttachmentDto>,
      taskIdentifier: TaskId
  ): TaskAttachmentListResource {
    if (attachments.isEmpty()) {
      return buildTaskAttachmentListResource(emptyList())
    }
    val tasksWithDeletePermission =
        taskAuthorizationComponent.filterTasksWithDeletePermission(setOf(taskIdentifier))
    val attachmentResources =
        attachmentResourceFactoryHelper.build(attachments, tasksWithDeletePermission)
    return buildTaskAttachmentListResource(attachmentResources)
  }

  private fun buildTaskAttachmentListResource(
      attachmentResources: Collection<AttachmentResource>
  ): TaskAttachmentListResource = TaskAttachmentListResource(attachmentResources)
}
