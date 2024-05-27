/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.boundary.WorkAreaListService
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromWorkAreaListEvent(private val workAreaListService: WorkAreaListService) :
    AbstractStateStrategy<WorkAreaListEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) = record.value is WorkAreaListEventAvro

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: WorkAreaListEventAvro): Unit =
      event.aggregate.run {
        workAreaListService.save(
            WorkAreaList(
                identifier = buildAggregateIdentifier(),
                projectIdentifier = getProjectIdentifier(),
                workAreas = workAreas.map { it.identifier.toUUID() }))
      }
}
