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
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.boundary.WorkAreaService
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromWorkAreaDeletedEvent(private val workAreaService: WorkAreaService) :
    AbstractStateStrategy<WorkAreaEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    return record.value is WorkAreaEventAvro &&
        (record.value as WorkAreaEventAvro).name == WorkAreaEventEnumAvro.DELETED
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: WorkAreaEventAvro) =
      workAreaService.deleteWorkArea(event.getIdentifier(), messageKey.rootContextIdentifier)
}
