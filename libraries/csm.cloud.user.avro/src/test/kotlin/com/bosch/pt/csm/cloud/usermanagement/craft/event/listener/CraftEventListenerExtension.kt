/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.event.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.referencedata.craft.event.listener.CraftEventListener
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.event.randomCraft
import io.mockk.mockk
import io.mockk.verify
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

fun CraftEventListener.submitCraft(
    existingCraft: CraftAggregateAvro? = null,
    event: CraftEventEnumAvro = CraftEventEnumAvro.CREATED,
    vararg blocks: ((CraftAggregateAvro) -> Unit)?
): CraftAggregateAvro {
  val craft = existingCraft.buildEventAvro(event, *blocks)

  val aggregateIdentifierAvro = craft.getAggregate().getAggregateIdentifier()
  val key =
      AggregateEventMessageKey(
          aggregateIdentifierAvro.buildAggregateIdentifier(),
          aggregateIdentifierAvro.getIdentifier().toUUID())

  return submitCraftEvent(craft, key).getAggregate()
}

fun <T : SpecificRecordBase> CraftEventListener.submitCraftEvent(
    value: T,
    key: EventMessageKey?
): T = submitEvent(value, key, ::listenToCraftEvents)

@Suppress("unused")
fun <V : SpecificRecordBase?> CraftEventListener.submitEvent(
    value: V,
    key: EventMessageKey?,
    listener: (ConsumerRecord<EventMessageKey, SpecificRecordBase?>, Acknowledgment) -> Unit
): V {
  mockk<Acknowledgment>(relaxed = true).apply {
    listener(ConsumerRecord("", 0, 0, key, value), this)
    verify { acknowledge() }
  }
  return value
}

private fun CraftAggregateAvro?.buildEventAvro(
    event: CraftEventEnumAvro,
    vararg blocks: ((CraftAggregateAvro) -> Unit)?
) =
    (this?.let { CraftEventAvro(event, this) } ?: randomCraft(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }
