/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.boundary.MilestoneService
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromMilestoneDeleteEvent(private val milestoneService: MilestoneService) :
    AbstractStateStrategy<MilestoneEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean =
      record.value is MilestoneEventAvro && (record.value as MilestoneEventAvro).name == DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: MilestoneEventAvro) {
    milestoneService.deleteMilestone(event.getIdentifier(), messageKey.rootContextIdentifier)
  }
}
