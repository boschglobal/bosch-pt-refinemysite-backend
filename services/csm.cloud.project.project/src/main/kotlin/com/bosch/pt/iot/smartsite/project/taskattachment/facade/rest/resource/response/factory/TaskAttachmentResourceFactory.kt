/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
open class TaskAttachmentResourceFactory(
    messageSource: MessageSource,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val taskAttachmentResourceFactoryHelper: TaskAttachmentResourceFactoryHelper
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(attachment: AttachmentDto): TaskAttachmentResource =
      taskAttachmentResourceFactoryHelper.build(
          attachment,
          taskAuthorizationComponent.hasDeletePermissionOnTask(attachment.taskIdentifier))
}
