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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitRelation(
    asReference: String = "relation",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: RelationEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((RelationAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingRelation = get<RelationAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((RelationAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((RelationAggregateAvro.Builder) -> Unit) = {
    it.source = it.source ?: getContext().lastIdentifierPerType[TASK.value]
    it.target = it.target ?: getContext().lastIdentifierPerType[MILESTONE.value]
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val relationEvent =
      existingRelation.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          relationEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          relationEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as RelationEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun RelationAggregateAvro?.buildEventAvro(
    eventType: RelationEventEnumAvro,
    vararg blocks: ((RelationAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { RelationEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newRelation(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newRelation(event: RelationEventEnumAvro = CREATED): RelationEventAvro.Builder {
  val relation =
      RelationAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.RELATION.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setType(RelationTypeEnumAvro.FINISH_TO_START)

  return RelationEventAvro.newBuilder().setAggregateBuilder(relation).setName(event)
}
