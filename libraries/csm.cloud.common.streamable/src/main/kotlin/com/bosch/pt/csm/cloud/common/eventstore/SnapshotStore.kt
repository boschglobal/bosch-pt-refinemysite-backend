/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import org.apache.avro.specific.SpecificRecordBase

/**
 * Interface to be implemented by a snapshot store to handle messages (and tombstone messages)
 * published on a local event bus.
 */
interface SnapshotStore {

  /** Returns true if a snapshot store handles a specific message. */
  fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean

  /**
   * Returns true if the snapshot store is able to handle tombstone messages of the given type.
   * Default is false since this is most of the time not required.
   */
  fun handlesTombstoneMessage(key: AggregateEventMessageKey): Boolean = false

  /**
   * Receives the message from the dispatcher in case it is a message supported by this snapshot
   * store.
   */
  fun handleMessage(
      key: AggregateEventMessageKey,
      message: SpecificRecordBase,
      source: EventSourceEnum
  )

  /**
   * Receives the tombstone message from the dispatcher in case they are supported by this snapshot
   * store
   */
  fun handleTombstoneMessage(key: AggregateEventMessageKey) {
    throw UnsupportedOperationException("Tombstone Messages not supported by this snapshot store")
  }
}
