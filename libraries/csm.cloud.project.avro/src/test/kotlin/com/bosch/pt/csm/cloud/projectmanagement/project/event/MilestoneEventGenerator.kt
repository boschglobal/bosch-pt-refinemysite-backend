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
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitMilestone(
    asReference: String = "milestone",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: MilestoneEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((MilestoneAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingMilestone = get<MilestoneAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((MilestoneAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((MilestoneAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val milestoneEvent =
      existingMilestone.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          milestoneEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          milestoneEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as MilestoneEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun MilestoneAggregateAvro?.buildEventAvro(
    eventType: MilestoneEventEnumAvro,
    vararg blocks: ((MilestoneAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { MilestoneEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newMilestone(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newMilestone(event: MilestoneEventEnumAvro = CREATED): MilestoneEventAvro.Builder {
  val milestone =
      MilestoneAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.MILESTONE.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setType(MilestoneTypeEnumAvro.PROJECT)
          .setHeader(true)
          .setName(randomString())
          .setDate(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())

  return MilestoneEventAvro.newBuilder().setAggregateBuilder(milestone).setName(event)
}
