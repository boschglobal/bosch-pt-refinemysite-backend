/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.boundary.TaskScheduleService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromTaskScheduleDeletedEvent(
    private val taskScheduleService: TaskScheduleService
) : AbstractStateStrategy<TaskScheduleEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is TaskScheduleEventAvro &&
          (record.value as TaskScheduleEventAvro).getName() == TaskScheduleEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: TaskScheduleEventAvro) =
      taskScheduleService.deleteTaskSchedule(
          event.getIdentifier(), messageKey.rootContextIdentifier)
}
