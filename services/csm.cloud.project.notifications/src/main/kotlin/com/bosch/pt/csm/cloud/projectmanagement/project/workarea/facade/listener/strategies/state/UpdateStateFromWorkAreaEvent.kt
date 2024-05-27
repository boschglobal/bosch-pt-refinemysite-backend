/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.boundary.WorkAreaService
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromWorkAreaEvent(private val workAreaService: WorkAreaService) :
    AbstractStateStrategy<WorkAreaEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is WorkAreaEventAvro &&
          (record.value as WorkAreaEventAvro).name != WorkAreaEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: WorkAreaEventAvro): Unit =
      event.aggregate.run {
        workAreaService.save(
            WorkArea(
                identifier = buildAggregateIdentifier(),
                projectIdentifier = messageKey.rootContextIdentifier,
                name = name))
      }
}
