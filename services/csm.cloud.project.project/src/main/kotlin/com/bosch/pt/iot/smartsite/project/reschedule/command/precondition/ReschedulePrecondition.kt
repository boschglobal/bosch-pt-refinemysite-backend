/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.precondition

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.RESCHEDULE_VALIDATION_ERROR_INVALID_SHIFT_DAYS

object ReschedulePrecondition {

  fun assertShiftNotZero(shiftDays: Long) {
    if (shiftDays == 0L) {
      throw PreconditionViolationException(RESCHEDULE_VALIDATION_ERROR_INVALID_SHIFT_DAYS)
    }
  }
}
