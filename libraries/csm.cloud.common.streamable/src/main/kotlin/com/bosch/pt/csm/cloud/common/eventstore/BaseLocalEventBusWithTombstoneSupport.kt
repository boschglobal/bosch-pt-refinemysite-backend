/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.ONLINE
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import org.springframework.context.ApplicationEventPublisher

/**
 * Generic implementation of a [LocalEventBus] that also supports tombstone message. See
 * [BaseLocalEventBus] for further details.
 *
 * @param ES: Implementation of [AbstractEventStore] used for the instance of this bus
 * @param S: Marker interface derived from [SnapshotStore] used to inject the corresponding snapshot
 *   stores relevant for this context
 */
open class BaseLocalEventBusWithTombstoneSupport<ES : AbstractEventStore<*>, S : SnapshotStore>(
    private val eventStore: ES,
    private val snapshotStores: List<S>,
    private val eventPublisher: ApplicationEventPublisher,
    private val avroEventMappers: List<AvroEventMapper> = emptyList()
) : BaseLocalEventBus<ES, S>(eventStore, snapshotStores, eventPublisher, avroEventMappers) {

  override fun emitTombstone(key: AggregateEventMessageKey, source: EventSourceEnum) {
    dispatchToSnapshotStore(key)
    if (source == ONLINE) {
      eventStore.save(key)
      eventPublisher.publishEvent(key)
    }
  }

  private fun dispatchToSnapshotStore(key: AggregateEventMessageKey) {
    snapshotStores
        .filter { it.handlesTombstoneMessage(key) }
        .map { it.handleTombstoneMessage(key) }
        .count()
        .apply {
          if (this < 1) {
            error("No snapshot store handled tombstone message with key $key")
          } else if (this > 1) {
            error("More than one snapshot store handled tombstone message with key $key")
          }
        }
  }
}

@Deprecated("Use BaseLocalEventBusWithTombstoneSupport instead.")
typealias AbstractLocalEventBusWithTombstoneSupport<ES, S> =
    BaseLocalEventBusWithTombstoneSupport<ES, S>
