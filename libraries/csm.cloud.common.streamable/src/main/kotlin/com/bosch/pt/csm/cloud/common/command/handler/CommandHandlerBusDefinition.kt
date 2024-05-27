/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBus
import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBusWithTombstoneSupport

interface CommandHandlerBusDefinition<T : VersionedSnapshot> {
  fun to(
      eventBus: BaseLocalEventBusWithTombstoneSupport<*, *>
  ): CommandHandlerSideEffectDefinition<T>
  fun to(eventBus: BaseLocalEventBus<*, *>): CommandHandlerSideEffectDefinition<T>
}
