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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RFVCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.BAD_WEATHER
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitRfvCustomization(
    asReference: String = "rfvCustomization",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: RfvCustomizationEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((RfvCustomizationAggregateAvro.Builder) -> Unit)? = null,
): EventStreamGenerator {
  val existingRfv = get<RfvCustomizationAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((RfvCustomizationAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((RfvCustomizationAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val rfvEvent =
      existingRfv.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          rfvEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          rfvEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as RfvCustomizationEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun RfvCustomizationAggregateAvro?.buildEventAvro(
    eventType: RfvCustomizationEventEnumAvro,
    vararg blocks: ((RfvCustomizationAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { RfvCustomizationEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newRfv(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newRfv(
    event: RfvCustomizationEventEnumAvro = CREATED
): RfvCustomizationEventAvro.Builder {
  val rfv =
      RfvCustomizationAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(RFVCUSTOMIZATION.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setKey(BAD_WEATHER)
          .setActive(true)

  return RfvCustomizationEventAvro.newBuilder().setAggregateBuilder(rfv).setName(event)
}
