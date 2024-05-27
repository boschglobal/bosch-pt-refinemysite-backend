/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot

interface CommandHandlerEmitTombstoneDefinition<T : VersionedSnapshot> {
  fun emitTombstone(): CommandHandlerBusDefinition<T>
}
