/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException

interface CommandHandlerExceptionDefinition<T : VersionedSnapshot> {

  /** Defines the exception to be thrown upon a failure of the preceding precondition */
  fun onFailureThrow(exceptionProducer: () -> Exception): CommandHandlerChangeDefinition<T>

  /**
   * Defines the exception message to be used for a [PreconditionViolationException] thrown on
   * failure of the preceding precondition
   */
  fun onFailureThrow(failureMessageKey: String): CommandHandlerChangeDefinition<T>
}
