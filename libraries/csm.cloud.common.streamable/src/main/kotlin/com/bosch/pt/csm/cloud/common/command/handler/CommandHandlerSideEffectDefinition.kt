/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot

interface CommandHandlerSideEffectDefinition<T : VersionedSnapshot> :
    CommandHandlerReturnSnapshotDefinition<T> {

  /**
   * Used to wrap calls to a remote system or calls that change something outside of the aggregate
   * (for example). This should always be used with great care considering potential problems when
   * the side effects fail. Verify first, if an event listener may not be a better option here.
   */
  fun withSideEffects(block: (T) -> Unit): CommandHandlerReturnSnapshotDefinition<T>
}
