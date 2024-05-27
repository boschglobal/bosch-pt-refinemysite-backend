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
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.toAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.service.MilestoneService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromMilestoneListEvent(private val milestoneService: MilestoneService) :
    AbstractStateStrategy<MilestoneListEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MilestoneListEventAvro && value.getName() != DELETED

  /**
   * The milestone list is not represented by its own entity in the activity service we just add the
   * position information to the milestone entity
   */
  @Trace
  override fun updateState(key: EventMessageKey, event: MilestoneListEventAvro) =
      event.getAggregate().getMilestones().forEachIndexed { index, ms ->
        milestoneService.updatePosition(
            event.getProjectIdentifier(), ms.toAggregateIdentifier(), index)
      }
}
