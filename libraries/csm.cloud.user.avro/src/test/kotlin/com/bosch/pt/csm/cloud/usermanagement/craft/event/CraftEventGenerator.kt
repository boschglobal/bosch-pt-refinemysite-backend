/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.event

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.referencedata.craft.common.CraftAggregateTypeEnum
import java.time.Instant
import java.util.Locale

@JvmOverloads
fun EventStreamGenerator.submitCraft(
    asReference: String = "craft",
    auditUserReference: String = DEFAULT_USER,
    eventType: CraftEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((CraftAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingCraft = get<CraftAggregateAvro>(asReference)

  val defaultAggregateModifications: ((CraftAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
  }

  val craftEvent =
      existingCraft.buildEventAvro(eventType, defaultAggregateModifications, aggregateModifications)

  val sentEvent =
      send("craft", asReference, null, craftEvent, time.toEpochMilli()) as CraftEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun CraftAggregateAvro?.buildEventAvro(
    eventType: CraftEventEnumAvro,
    vararg blocks: ((CraftAggregateAvro.Builder) -> Unit)?
): CraftEventAvro =
    (this?.let { CraftEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newCraft(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newCraft(event: CraftEventEnumAvro = CREATED): CraftEventAvro.Builder {
  val defaultName = randomString()
  val craft =
      CraftAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(CraftAggregateTypeEnum.CRAFT.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setDefaultName(defaultName)
          .setTranslations(
              listOf(
                  CraftTranslationAvro.newBuilder()
                      .setLocale(Locale.GERMANY.toString())
                      .setValue(defaultName)
                      .build(),
                  CraftTranslationAvro.newBuilder()
                      .setLocale(Locale.UK.toString())
                      .setValue(randomString())
                      .build()))

  return CraftEventAvro.newBuilder().setAggregateBuilder(craft).setName(event)
}
