/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot

interface CommandHandlerEmitEventDefinition<T : VersionedSnapshot> {
  fun emitEvent(eventType: Enum<*>): CommandHandlerDirtyCheckDefinition<T>
  fun emitEvent(event: Event): CommandHandlerDirtyCheckDefinition<T>
  fun emitEvents(block: (snapshot: T) -> List<Event>): CommandHandlerBusDefinition<T>

  fun emitEvent(block: (snapshot: T) -> Event): CommandHandlerBusDefinition<T> =
      this.emitEvents { listOf(block.invoke(it)) }
}

typealias Event = Any
