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
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.context.ApplicationEventPublisher

/**
 * Generic implementation of a [LocalEventBus]. To create a sub-class of it, just specify your
 * implementation of the event store and the marker interfaces used to search for all snapshot
 * stores relevant for your sub-class of the [LocalEventBus].
 *
 * @param ES: Implementation of [AbstractEventStore] used for the instance of this bus
 * @param S: Marker interface derived from [SnapshotStore] used to inject the corresponding snapshot
 *   stores relevant for this context
 */
open class BaseLocalEventBus<ES : AbstractEventStore<*>, S : SnapshotStore>(
    private val eventStore: ES,
    private val snapshotStores: List<S>,
    private val eventPublisher: ApplicationEventPublisher,
    private val avroEventMappers: List<AvroEventMapper> = emptyList()
) : LocalEventBus {

  override fun emit(event: Any, newVersion: Long, source: EventSourceEnum) {
    try {
      val mapper = avroEventMappers.single { it.canMap(event) }
      emit(mapper.mapToKey(event, newVersion), mapper.mapToValue(event, newVersion), source)
    } catch (e: NoSuchElementException) {
      throw NoSuchElementException(
          "No avro event mapper found for ${event::class.java}. " +
              "Did you forget to register the mapper on the event bus?",
          e)
    } catch (e: IllegalArgumentException) {
      throw IllegalArgumentException(
          "Multiple avro event mappers found for ${event::class.java}.", e)
    }
  }

  override fun emit(
      key: AggregateEventMessageKey,
      value: SpecificRecordBase,
      source: EventSourceEnum
  ) {
    dispatchToSnapshotStore(key, value, source)
    if (source == ONLINE) {
      eventStore.save(key, value)
      eventPublisher.publishEvent(value)
    }
  }

  private fun dispatchToSnapshotStore(
      key: AggregateEventMessageKey,
      message: SpecificRecordBase,
      source: EventSourceEnum
  ) {
    snapshotStores
        .filter { it.handlesMessage(key, message) }
        .map { it.handleMessage(key, message, source) }
        .count()
        .apply {
          if (this < 1) {
            error("No snapshot store handled message with key $key")
          } else if (this > 1) {
            error("More than one snapshot store handled message with key $key")
          }
        }
  }
}

@Deprecated("Use BaseLocalEventBus instead.")
typealias AbstractLocalEventBus<ES, S> = BaseLocalEventBus<ES, S>
