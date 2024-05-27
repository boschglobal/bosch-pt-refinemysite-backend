/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.storagemanagement.storage.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.storage.event.messages.FileCreatedEventAvro
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase

@JvmOverloads
fun EventStreamGenerator.fileCreated(
    asReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((FileCreatedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  val defaultAggregateModifications: ((FileCreatedEventAvro.Builder) -> Unit) = {}

  val event =
      FileCreatedEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications?.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

private fun EventStreamGenerator.sendEvent(
    asReference: String,
    event: SpecificRecordBase,
    time: Instant
) {
  val sentEvent =
      send("storage", asReference, null, event, time.toEpochMilli()) as SpecificRecordBase
  getContext().events[asReference] = sentEvent
  getContext().lastRootContextIdentifier =
      sentEvent.get("aggregateIdentifier") as AggregateIdentifierAvro
}
