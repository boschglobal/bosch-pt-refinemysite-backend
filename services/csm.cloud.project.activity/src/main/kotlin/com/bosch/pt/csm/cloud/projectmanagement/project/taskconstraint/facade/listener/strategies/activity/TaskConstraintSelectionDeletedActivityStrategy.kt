/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service.TaskConstraintSelectionService
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro as TaskConstraintSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

@Component
class TaskConstraintSelectionDeletedActivityStrategy(
    private val taskConstraintSelectionService: TaskConstraintSelectionService,
    private val idGenerator: IdGenerator
) : AbstractTaskConstraintSelectionActivityStrategy() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskConstraintSelectionEventAvro &&
          value.getName() == DELETED &&
          getConstraintsOfPreviousVersion(value.getAggregate(), key.rootContextIdentifier)
              .isNotEmpty()

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskConstraintSelectionEventAvro): Activity {
    val aggregate = event.getAggregate()
    val projectIdentifier = key.rootContextIdentifier
    val deletedConstraints = getConstraintsOfPreviousVersion(aggregate, projectIdentifier)

    return Activity(
        aggregateIdentifier = aggregate.buildAggregateIdentifier(),
        identifier = idGenerator.generateId(),
        event = aggregate.buildEventInformation(event),
        summary =
            buildSummary(TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED, projectIdentifier, event),
        details = buildDetails(deletedConstraints),
        context = Context(project = projectIdentifier, task = aggregate.buildTaskIdentifier()))
  }

  private fun buildDetails(deletedConstraints: Set<TaskConstraintEnum>): Details =
      AttributeChanges(
          deletedConstraints.map {
            ChangeDescription(
                TASK_ACTION_SELECTION_ACTIVITY_REMOVED, listOf(mapToMessageKeyValue(it)))
          })

  private fun getConstraintsOfPreviousVersion(
      aggregate: TaskConstraintSelectionAggregateAvro?,
      projectIdentifier: UUID
  ): Set<TaskConstraintEnum> {
    val identifier = aggregate!!.getAggregateIdentifier().getIdentifier().toUUID()
    val version = aggregate.getAggregateIdentifier().getVersion()

    val previousVersion =
        taskConstraintSelectionService.find(identifier, version - 1, projectIdentifier)

    return previousVersion!!.actions.sortedBy { taskConstraintOrder.indexOf(it) }.toSet()
  }
}
