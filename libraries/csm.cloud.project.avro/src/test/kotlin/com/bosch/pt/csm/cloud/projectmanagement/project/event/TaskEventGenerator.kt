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
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTask(
    asReference: String = "task",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TaskEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TaskAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingTask = get<TaskAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((TaskAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((TaskAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
    it.craft = it.craft ?: getContext().lastIdentifierPerType[PROJECTCRAFT.value]
  }

  val taskEvent =
      existingTask.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          taskEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          taskEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as TaskEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TaskAggregateAvro?.buildEventAvro(
    eventType: TaskEventEnumAvro,
    vararg blocks: ((TaskAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { TaskEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newTask(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newTask(event: TaskEventEnumAvro = CREATED): TaskEventAvro.Builder {
  val date = LocalDate.now()
  val task =
      TaskAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setDescription(randomString())
          .setEditDate(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
          .setLocation(randomString())
          .setStatus(TaskStatusEnumAvro.OPEN)
          .setName(randomString())

  return TaskEventAvro.newBuilder().setAggregateBuilder(task).setName(event)
}
