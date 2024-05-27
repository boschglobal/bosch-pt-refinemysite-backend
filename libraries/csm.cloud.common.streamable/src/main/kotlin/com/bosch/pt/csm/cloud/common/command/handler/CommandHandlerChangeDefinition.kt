/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import org.springframework.security.access.AccessDeniedException

interface CommandHandlerChangeDefinition<T : VersionedSnapshot> :
    CommandHandlerEmitEventDefinition<T>, CommandHandlerEmitTombstoneDefinition<T> {

  /** asserts that the given version matches the version of the snapshot */
  fun assertVersionMatches(version: Long): CommandHandlerChangeDefinition<T>

  /**
   * Checks authorization and - in the next step - defines a message to be used for an
   * [AccessDeniedException] that is thrown in case this check returns false
   */
  fun checkAuthorization(precondition: (T) -> Boolean): CommandHandlerExceptionDefinition<T>

  /**
   * Checks a precondition and - in the next step - defines an exception to be thrown upon failure
   */
  fun checkPrecondition(precondition: (T) -> Boolean): CommandHandlerExceptionDefinition<T>

  /** Used to wrap instructions that modify the current snapshot. */
  fun applyChanges(block: (T) -> Unit): CommandHandlerEmitEventDefinition<T>

  /** Returns a modified copy of the current snapshot. */
  fun update(block: (T) -> T): CommandHandlerEmitEventDefinition<T>
}
