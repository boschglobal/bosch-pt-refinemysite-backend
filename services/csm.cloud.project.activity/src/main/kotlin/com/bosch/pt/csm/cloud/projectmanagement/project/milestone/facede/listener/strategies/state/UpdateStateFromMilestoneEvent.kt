/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facede.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facede.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.service.MilestoneService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromMilestoneEvent(private val milestoneService: MilestoneService) :
    AbstractStateStrategy<MilestoneEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MilestoneEventAvro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: MilestoneEventAvro) {
    val projectIdentifier = key.rootContextIdentifier
    val milestoneAggregate = event.getAggregate()
    val milestone = milestoneAggregate.toEntity(projectIdentifier)
    val version = milestoneAggregate.getVersion()
    val identifier = milestoneAggregate.getIdentifier()

    if (version > 0) {
      val previousMilestone =
          milestoneService.findMilestone(identifier, projectIdentifier, version - 1)
      if (milestoneNotMoved(milestone, previousMilestone)) {
        milestone.position = previousMilestone.position
      }
    }

    milestoneService.save(milestone)
    milestoneService.deleteByVersion(identifier, version - 2, projectIdentifier)
  }

  private fun milestoneNotMoved(milestone: Milestone, previousMilestoneVersion: Milestone) =
      milestone.date == previousMilestoneVersion.date &&
          milestone.header == previousMilestoneVersion.header &&
          milestone.workAreaIdentifier == previousMilestoneVersion.workAreaIdentifier
}
