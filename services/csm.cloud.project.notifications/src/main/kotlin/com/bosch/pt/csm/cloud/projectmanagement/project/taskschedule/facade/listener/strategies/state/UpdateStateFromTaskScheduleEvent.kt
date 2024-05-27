/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.boundary.TaskScheduleService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskScheduleSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskIdentifier
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromTaskScheduleEvent(private val taskScheduleService: TaskScheduleService) :
    AbstractStateStrategy<TaskScheduleEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is TaskScheduleEventAvro &&
          (record.value as TaskScheduleEventAvro).getName() != TaskScheduleEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: TaskScheduleEventAvro): Unit =
      event.getAggregate().run {
        taskScheduleService.save(
            TaskSchedule(
                taskIdentifier = getTaskIdentifier(),
                identifier = buildAggregateIdentifier(),
                projectIdentifier = messageKey.rootContextIdentifier,
                end = getEnd()?.toLocalDateByMillis(),
                start = getStart()?.toLocalDateByMillis(),
                slots =
                    getSlots().map {
                      TaskScheduleSlot(
                          it.getDate().toLocalDateByMillis(),
                          it.getDayCard().getIdentifier().toUUID())
                    }))
      }
}
