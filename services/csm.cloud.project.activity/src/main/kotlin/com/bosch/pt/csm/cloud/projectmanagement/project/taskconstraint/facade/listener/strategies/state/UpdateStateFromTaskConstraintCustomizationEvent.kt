/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service.TaskConstraintCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromTaskConstraintCustomizationEvent(
    private val constraintCustomizationService: TaskConstraintCustomizationService
) : AbstractStateStrategy<TaskConstraintCustomizationEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskConstraintCustomizationEventAvro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: TaskConstraintCustomizationEventAvro) =
      event.getAggregate().run {
        val projectIdentifier = key.rootContextIdentifier

        constraintCustomizationService.save(toEntity())
        constraintCustomizationService.deleteByVersion(
            getIdentifier(), getAggregateIdentifier().getVersion() - 1, projectIdentifier)
      }
}
