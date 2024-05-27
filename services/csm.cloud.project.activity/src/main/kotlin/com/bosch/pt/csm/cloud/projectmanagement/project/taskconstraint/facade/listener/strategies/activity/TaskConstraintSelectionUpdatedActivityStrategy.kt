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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_ADDED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.buildTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service.TaskConstraintSelectionService
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro as TaskConstraintSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

@Component
class TaskConstraintSelectionUpdatedActivityStrategy(
    private val taskConstraintSelectionService: TaskConstraintSelectionService,
    private val idGenerator: IdGenerator
) : AbstractTaskConstraintSelectionActivityStrategy() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskConstraintSelectionEventAvro && value.getName() == UPDATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskConstraintSelectionEventAvro): Activity {
    val aggregate = event.getAggregate()
    val projectIdentifier = key.rootContextIdentifier
    val changedAttributes = differenceToPrevious(aggregate, projectIdentifier)

    return Activity(
        aggregateIdentifier = aggregate.buildAggregateIdentifier(),
        identifier = idGenerator.generateId(),
        event = aggregate.buildEventInformation(event),
        summary =
            buildSummary(TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED, projectIdentifier, event),
        details = buildDetails(changedAttributes),
        context = Context(project = projectIdentifier, task = aggregate.buildTaskIdentifier()))
  }

  private fun buildDetails(
      changedAttributes: Map<AttributeChangeEnum, Set<TaskConstraintEnum>>
  ): Details =
      AttributeChanges(
          changedAttributes.flatMap { (changeType, values) ->
            values.map {
              ChangeDescription(mapToMessageKey(changeType), listOf(mapToMessageKeyValue(it)))
            }
          })

  private fun mapToMessageKey(event: AttributeChangeEnum): String {
    return when (event) {
      REMOVED -> TASK_ACTION_SELECTION_ACTIVITY_REMOVED
      CREATED -> TASK_ACTION_SELECTION_ACTIVITY_ADDED
      else -> throw IllegalStateException("Can not handle AttributeChangeEnum of value $event")
    }
  }

  private fun differenceToPrevious(
      aggregate: TaskConstraintSelectionAggregateAvro?,
      projectIdentifier: UUID
  ): Map<AttributeChangeEnum, Set<TaskConstraintEnum>> {
    val identifier = aggregate!!.getAggregateIdentifier().getIdentifier().toUUID()
    val version = aggregate.getAggregateIdentifier().getVersion()

    val currentVersion = taskConstraintSelectionService.find(identifier, version, projectIdentifier)
    val previousVersion =
        taskConstraintSelectionService.find(identifier, version - 1, projectIdentifier)

    return mapOf(
        REMOVED to
            previousVersion!!
                .actions
                .subtract(currentVersion!!.actions)
                .sortedBy { taskConstraintOrder.indexOf(it) }
                .toSet(),
        CREATED to
            currentVersion
                .actions
                .subtract(previousVersion.actions)
                .sortedBy { taskConstraintOrder.indexOf(it) }
                .toSet())
  }
}
