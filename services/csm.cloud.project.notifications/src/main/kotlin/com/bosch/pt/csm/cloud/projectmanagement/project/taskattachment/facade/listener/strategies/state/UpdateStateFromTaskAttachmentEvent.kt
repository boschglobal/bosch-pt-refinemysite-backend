/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.Attachment
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.model.TaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getTaskIdentifier
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromTaskAttachmentEvent(private val taskAttachmentService: TaskAttachmentService) :
    AbstractStateStrategy<TaskAttachmentEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is TaskAttachmentEventAvro &&
          (record.value as TaskAttachmentEventAvro).getName() != TaskAttachmentEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: TaskAttachmentEventAvro): Unit =
      event.getAggregate().run {
        taskAttachmentService.save(
            TaskAttachment(
                identifier = buildAggregateIdentifier(),
                projectIdentifier = messageKey.rootContextIdentifier,
                taskIdentifier = getTaskIdentifier(),
                attachment = Attachment.fromAttachmentAvro(getAttachment())))
      }
}
