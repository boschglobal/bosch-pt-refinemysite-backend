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
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getIdentifier
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromTaskAttachmentDeletedEvent(
    private val taskAttachmentService: TaskAttachmentService
) : AbstractStateStrategy<TaskAttachmentEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is TaskAttachmentEventAvro &&
          (record.value as TaskAttachmentEventAvro).getName() == TaskAttachmentEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: TaskAttachmentEventAvro) =
      taskAttachmentService.deleteTaskAttachment(
          event.getIdentifier(), messageKey.rootContextIdentifier)
}
