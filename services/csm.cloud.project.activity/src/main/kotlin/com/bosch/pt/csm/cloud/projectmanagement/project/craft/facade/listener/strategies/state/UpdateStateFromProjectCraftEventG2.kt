/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.service.ProjectCraftService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromProjectCraftEventG2(private val projectCraftService: ProjectCraftService) :
    AbstractStateStrategy<ProjectCraftEventG2Avro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is ProjectCraftEventG2Avro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: ProjectCraftEventG2Avro) =
      event.getAggregate().run {
        val projectIdentifier = key.rootContextIdentifier

        projectCraftService.save(toEntity(projectIdentifier))
        projectCraftService.deleteByVersion(
            getIdentifier(), getAggregateIdentifier().getVersion() - 1, projectIdentifier)
      }
}
