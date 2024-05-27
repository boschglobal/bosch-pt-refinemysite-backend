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
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.boundary.ProjectCraftService
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromProjectCraftDeletedEvent(
    private val projectCraftService: ProjectCraftService
) : AbstractStateStrategy<ProjectCraftEventG2Avro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    return record.value is ProjectCraftEventG2Avro &&
        (record.value as ProjectCraftEventG2Avro).getName() == ProjectCraftEventEnumAvro.DELETED
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: ProjectCraftEventG2Avro) =
      projectCraftService.deleteProjectCraft(
          event.getIdentifier(), messageKey.rootContextIdentifier)
}
