/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.common.command

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandlerBusDefinition
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandlerChangeDefinition
import com.bosch.pt.csm.cloud.common.command.handler.Event
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot

@LibraryCandidate("Could be added to CommandHandlerChangeDefinition")
fun <T : VersionedSnapshot> CommandHandlerChangeDefinition<T>.emitSingleEvent(
    block: (snapshot: T) -> Event
): CommandHandlerBusDefinition<T> = this.emitEvents { listOf(block.invoke(it)) }
