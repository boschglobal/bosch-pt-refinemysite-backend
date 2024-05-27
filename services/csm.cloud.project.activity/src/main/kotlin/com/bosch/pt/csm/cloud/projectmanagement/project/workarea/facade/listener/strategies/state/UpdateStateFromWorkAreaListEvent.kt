/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.service.WorkAreaListService
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromWorkAreaListEvent(private val workAreaListService: WorkAreaListService) :
    AbstractStateStrategy<WorkAreaListEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is WorkAreaListEventAvro

  @Trace
  override fun updateState(key: EventMessageKey, event: WorkAreaListEventAvro): Unit =
      event.getAggregate().run { workAreaListService.save(toEntity()) }
}
