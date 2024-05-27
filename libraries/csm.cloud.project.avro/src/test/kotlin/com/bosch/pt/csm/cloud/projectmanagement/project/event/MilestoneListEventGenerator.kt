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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.CREATED
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitMilestoneList(
    asReference: String = "milestoneList",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: MilestoneListEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((MilestoneListAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingMilestoneList = get<MilestoneListAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((MilestoneListAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((MilestoneListAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
    if (it.milestones == null && getContext().lastIdentifierPerType[MILESTONE.value] != null) {
      it.milestones = listOf(getContext().lastIdentifierPerType[MILESTONE.value])
    }
  }

  val milestoneListEvent =
      existingMilestoneList.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          milestoneListEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          milestoneListEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as MilestoneListEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun MilestoneListAggregateAvro?.buildEventAvro(
    eventType: MilestoneListEventEnumAvro,
    vararg blocks: ((MilestoneListAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { MilestoneListEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newMilestoneList(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newMilestoneList(
    event: MilestoneListEventEnumAvro = CREATED
): MilestoneListEventAvro.Builder {
  val milestoneList =
      MilestoneListAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.MILESTONELIST.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setDate(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
          .setHeader(true)

  return MilestoneListEventAvro.newBuilder().setAggregateBuilder(milestoneList).setName(event)
}
