/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.query

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException

object QueryUtilities {

  fun <T : Set<*>> T.assertSize(expectedSize: Int, errorMessageKey: String): T {
    if (this.size != expectedSize) {
      throw PreconditionViolationException(errorMessageKey)
    }
    return this
  }
}
