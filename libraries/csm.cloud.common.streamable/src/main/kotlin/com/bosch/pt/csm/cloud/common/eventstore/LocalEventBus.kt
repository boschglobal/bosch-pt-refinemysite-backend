/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.ONLINE
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import org.apache.avro.specific.SpecificRecordBase

/**
 * The LocalEventBus is used by command handlers to publish events resulting from handling a command
 * message. It is called "local" because this bus does not directly distribute events to different
 * applications or application instances. It has three responsibilities:
 *
 * 1) Forward the event to the corresponding snapshot store of the aggregate to make sure, the
 * snapshot is updated.
 *
 * 2) Forward the event to the event store to make sure it is persisted. In our case, this event
 * store is usually a local database table used to store the published event in the same transaction
 * that the snapshot is updated in. This way, we guarantee, that updating the snapshot and
 * publishing of the event is atomic. The event is later taken from this event table and forwarded
 * to a kafka topic by using an external process that implements to "Outbox Pattern / Transactional
 * Outbox" (https://microservices.io/patterns/data/transactional-outbox.html).
 *
 * 3) Publish the message as a Spring Application Event so that local listeners can react on the
 * message in the same transaction as well. This can be used if strict consistency is required by an
 * event handler reacting to this message. If possible, consume the message from kafka and embrace
 * eventual consistency to avoid additional latency in the command handling request.
 *
 * Use either of the two abstract implementations [BaseLocalEventBus] or
 * [BaseLocalEventBusWithTombstoneSupport] to create your instance of the LocalEventBus.
 */
interface LocalEventBus {

  /**
   * This function receives an internal representation of an event (as a POJO), calls the
   * corresponding mapper and emits the AVRO representation of the event message (key and value) to
   * the event bus.
   */
  fun emit(event: Any, newVersion: Long, source: EventSourceEnum = ONLINE)

  /**
   * This function receives the AVRO representation of the event message (key and value) and
   * implements the above-mentioned steps that the event bus is responsible for.
   */
  fun emit(
      key: AggregateEventMessageKey,
      value: SpecificRecordBase,
      source: EventSourceEnum = ONLINE
  )

  fun emitTombstone(key: AggregateEventMessageKey, source: EventSourceEnum = ONLINE) {
    throw UnsupportedOperationException("This local event bus does not support tombstone messages")
  }
}
