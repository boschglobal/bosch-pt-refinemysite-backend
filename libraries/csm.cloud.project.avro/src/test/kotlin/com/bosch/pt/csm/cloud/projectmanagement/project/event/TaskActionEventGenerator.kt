/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTaskAction(
    asReference: String = "taskAction",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TaskActionSelectionEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TaskActionSelectionAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingTaskAction = get<TaskActionSelectionAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((TaskActionSelectionAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((TaskActionSelectionAggregateAvro.Builder) -> Unit) = {
    it.task = it.task ?: getContext().lastIdentifierPerType[TASK.value]
  }

  val taskActionEvent =
      existingTaskAction.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          taskActionEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          taskActionEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as TaskActionSelectionEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TaskActionSelectionAggregateAvro?.buildEventAvro(
    eventType: TaskActionSelectionEventEnumAvro,
    vararg blocks: ((TaskActionSelectionAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { TaskActionSelectionEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newTaskAction(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newTaskAction(
    event: TaskActionSelectionEventEnumAvro = CREATED
): TaskActionSelectionEventAvro.Builder {
  val task =
      TaskActionSelectionAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier((ProjectmanagementAggregateTypeEnum.TASKACTION.value)))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setActions(listOf(TaskActionEnumAvro.EQUIPMENT))

  return TaskActionSelectionEventAvro.newBuilder().setAggregateBuilder(task).setName(event)
}
