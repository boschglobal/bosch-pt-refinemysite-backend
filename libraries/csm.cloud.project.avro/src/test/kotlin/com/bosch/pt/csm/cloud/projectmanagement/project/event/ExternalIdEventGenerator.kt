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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.EXTERNAL_ID
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdTypeEnumAvro.MS_PROJECT
import java.time.Instant
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitExternalId(
    asReference: String = "externalId",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.identifier.toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: ExternalIdEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((ExternalIdAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingExternalId = get<ExternalIdAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((ExternalIdAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((ExternalIdAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val externalIdEvent =
      existingExternalId.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          externalIdEvent.aggregate.aggregateIdentifier.buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          externalIdEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as ExternalIdEventAvro
  getContext().events[asReference] = sentEvent.aggregate

  return this
}

private fun ExternalIdAggregateAvro?.buildEventAvro(
    eventType: ExternalIdEventEnumAvro,
    vararg blocks: ((ExternalIdAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { ExternalIdEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newExternalId(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newExternalId(event: ExternalIdEventEnumAvro = CREATED): ExternalIdEventAvro.Builder {
  val externalId =
      ExternalIdAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(EXTERNAL_ID.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setGuid(randomUUID().toString())
          .setFileUniqueId(1)
          .setFileId(1)
          .setType(MS_PROJECT)

  return ExternalIdEventAvro.newBuilder().setAggregateBuilder(externalId).setName(event)
}
