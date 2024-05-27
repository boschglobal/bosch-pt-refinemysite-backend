/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.project.service.ProjectService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromProjectEvent(private val projectService: ProjectService) :
    AbstractStateStrategy<ProjectEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is ProjectEventAvro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: ProjectEventAvro) =
      event.getAggregate().run {
        projectService.save(toEntity())
        projectService.deleteByVersion(getIdentifier(), getAggregateIdentifier().getVersion() - 1)
      }
}
