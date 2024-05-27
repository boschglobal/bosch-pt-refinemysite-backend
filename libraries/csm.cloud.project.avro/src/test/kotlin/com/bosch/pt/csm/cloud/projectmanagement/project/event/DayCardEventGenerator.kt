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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitDayCardG2(
    asReference: String = "dayCard",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: DayCardEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((DayCardAggregateG2Avro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingDayCard = get<DayCardAggregateG2Avro?>(asReference)

  val defaultAggregateModifications: ((DayCardAggregateG2Avro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((DayCardAggregateG2Avro.Builder) -> Unit) = {
    it.task = it.task ?: getContext().lastIdentifierPerType[TASK.value]
  }

  val manpowerModification: ((DayCardAggregateG2Avro.Builder) -> Unit) = { it.manpower.setScale(2) }

  val dayCardEvent =
      existingDayCard.buildEventAvro(
          eventType,
          defaultAggregateModifications,
          aggregateModifications,
          referenceModifications,
          manpowerModification)

  val messageKey =
      AggregateEventMessageKey(
          dayCardEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          dayCardEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as DayCardEventG2Avro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun DayCardAggregateG2Avro?.buildEventAvro(
    eventType: DayCardEventEnumAvro,
    vararg blocks: ((DayCardAggregateG2Avro.Builder) -> Unit)?
) =
    (this?.let { DayCardEventG2Avro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newProject(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newProject(event: DayCardEventEnumAvro = CREATED): DayCardEventG2Avro.Builder {
  val dayCard =
      DayCardAggregateG2Avro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.DAYCARD.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setManpower(1.0f.toBigDecimal())
          .setNotes(randomString())
          .setReason(DayCardReasonNotDoneEnumAvro.BAD_WEATHER)
          .setStatus(DayCardStatusEnumAvro.NOTDONE)
          .setTitle(randomString())

  return DayCardEventG2Avro.newBuilder().setAggregateBuilder(dayCard).setName(event)
}
