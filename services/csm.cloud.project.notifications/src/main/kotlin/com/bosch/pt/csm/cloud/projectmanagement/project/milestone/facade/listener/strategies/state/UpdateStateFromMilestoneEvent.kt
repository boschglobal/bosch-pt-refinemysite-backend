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
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.toAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.boundary.MilestoneService
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.listener.message.buildMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromMilestoneEvent(private val milestoneService: MilestoneService) :
    AbstractStateStrategy<MilestoneEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord): Boolean =
      record.value is MilestoneEventAvro && (record.value as MilestoneEventAvro).name != DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: MilestoneEventAvro) {
    val milestoneAggregate = event.aggregate
    val milestone = milestoneAggregate.buildMilestone()

    if (milestoneAggregate.getVersion() > 0) {
      val previousMilestoneVersion =
          milestoneService.findMilestone(
              milestoneAggregate.getProjectIdentifier(),
              milestoneAggregate.aggregateIdentifier
                  .toAggregateIdentifier()
                  .copy(version = milestoneAggregate.aggregateIdentifier.version - 1))
      if (milestoneNotMoved(milestone, previousMilestoneVersion!!)) {
        milestone.position = previousMilestoneVersion.position
      }
    }

    milestoneService.save(milestone)
  }

  private fun milestoneNotMoved(milestone: Milestone, previousMilestoneVersion: Milestone) =
      milestone.date == previousMilestoneVersion.date &&
          milestone.header == previousMilestoneVersion.header &&
          milestone.workAreaIdentifier == previousMilestoneVersion.workAreaIdentifier
}
