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
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.CREATED
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTaskSchedule(
    asReference: String = "taskSchedule",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TaskScheduleEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TaskScheduleAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingSchedule = get<TaskScheduleAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((TaskScheduleAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((TaskScheduleAggregateAvro.Builder) -> Unit) = {
    it.task = it.task ?: getContext().lastIdentifierPerType[TASK.value]
  }

  val scheduleEvent =
      existingSchedule.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          scheduleEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          scheduleEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as TaskScheduleEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TaskScheduleAggregateAvro?.buildEventAvro(
    eventType: TaskScheduleEventEnumAvro,
    vararg blocks: ((TaskScheduleAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { TaskScheduleEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newSchedule(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newSchedule(event: TaskScheduleEventEnumAvro = CREATED): TaskScheduleEventAvro.Builder {
  val date = LocalDate.now()
  val schedule =
      TaskScheduleAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.TASKSCHEDULE.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setEnd(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
          .setSlots(listOf())
          .setStart(date.atStartOfDay().minusDays(5).toInstant(ZoneOffset.UTC).toEpochMilli())

  return TaskScheduleEventAvro.newBuilder().setAggregateBuilder(schedule).setName(event)
}
