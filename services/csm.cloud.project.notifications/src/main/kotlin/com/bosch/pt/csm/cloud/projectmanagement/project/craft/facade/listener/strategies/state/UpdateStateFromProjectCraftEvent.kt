/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.boundary.ProjectCraftService
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromProjectCraftEvent(private val projectCraftService: ProjectCraftService) :
    AbstractStateStrategy<ProjectCraftEventG2Avro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is ProjectCraftEventG2Avro &&
          (record.value as ProjectCraftEventG2Avro).getName() != ProjectCraftEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: ProjectCraftEventG2Avro): Unit =
      event.getAggregate().run {
        projectCraftService.save(
            ProjectCraft(
                identifier = buildAggregateIdentifier(),
                projectIdentifier = getProjectIdentifier(),
                color = getColor(),
                name = getName()))
      }
}
