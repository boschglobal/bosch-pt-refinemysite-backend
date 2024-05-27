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
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.service.WorkAreaService
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromWorkAreaEvent(private val workAreaService: WorkAreaService) :
    AbstractStateStrategy<WorkAreaEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is WorkAreaEventAvro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: WorkAreaEventAvro) =
      event.getAggregate().run {
        val projectIdentifier = key.rootContextIdentifier

        workAreaService.save(toEntity(projectIdentifier))
        workAreaService.deleteByVersion(
            getIdentifier(), getAggregateIdentifier().getVersion() - 1, projectIdentifier)
      }
}
