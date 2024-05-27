/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildConstraintSelection
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service.TaskConstraintSelectionService
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

@Component
class UpdateStateFromTaskConstraintSelectionEvent(
    private val taskConstraintSelectionService: TaskConstraintSelectionService
) : AbstractStateStrategy<TaskConstraintSelectionEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskConstraintSelectionEventAvro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: TaskConstraintSelectionEventAvro) =
      event.getAggregate().run {
        val projectIdentifier = key.rootContextIdentifier

        taskConstraintSelectionService.save(this.buildConstraintSelection(projectIdentifier))
        taskConstraintSelectionService.deleteByVersion(
            getAggregateIdentifier().getIdentifier().toUUID(),
            getAggregateIdentifier().getVersion() - 2,
            projectIdentifier)
      }
}
