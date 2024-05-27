/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.BARE_PARAMETERS_ONE_PARAMETER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.CREATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro as TaskConstraintSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

@Component
class TaskConstraintSelectionCreatedActivityStrategy(private val idGenerator: IdGenerator) :
    AbstractTaskConstraintSelectionActivityStrategy() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskConstraintSelectionEventAvro &&
          value.getName() == CREATED &&
          value.getAggregate().getActions().isNotEmpty()

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskConstraintSelectionEventAvro): Activity {
    val aggregate: TaskConstraintSelectionAggregateAvro = event.getAggregate()
    val projectIdentifier = key.rootContextIdentifier

    return Activity(
        aggregateIdentifier = aggregate.buildAggregateIdentifier(),
        identifier = idGenerator.generateId(),
        event = aggregate.buildEventInformation(event),
        summary = buildSummary(TASK_ACTION_SELECTION_ACTIVITY_CREATED, projectIdentifier, event),
        details = buildDetails(aggregate),
        context = Context(project = projectIdentifier, task = aggregate.buildTaskIdentifier()))
  }

  private fun buildDetails(aggregate: TaskConstraintSelectionAggregateAvro) =
      AttributeChanges(
          aggregate
              .getActions()
              .map { TaskConstraintEnum.valueOf(it.name) }
              .sortedBy { taskConstraintOrder.indexOf(it) }
              .map {
                ChangeDescription(BARE_PARAMETERS_ONE_PARAMETER, listOf(mapToMessageKeyValue(it)))
              })
}
