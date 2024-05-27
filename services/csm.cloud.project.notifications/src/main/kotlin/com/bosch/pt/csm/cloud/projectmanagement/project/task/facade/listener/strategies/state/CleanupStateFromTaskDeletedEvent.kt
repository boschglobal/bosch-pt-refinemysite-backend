/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromTaskDeletedEvent(private val taskService: TaskService) :
    AbstractStateStrategy<TaskEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is TaskEventAvro &&
          (record.value as TaskEventAvro).name == TaskEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: TaskEventAvro) =
      event.run {
        taskService.deleteTaskAndAllRelatedDocuments(
            getIdentifier(), messageKey.rootContextIdentifier)
      }
}
