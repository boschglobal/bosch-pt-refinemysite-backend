/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.service.precondition

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.DONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN

object DayCardPrecondition {

  fun isCancelPossible(status: DayCardStatusEnum, reviewPermission: Boolean): Boolean =
      status === OPEN || status === DONE && reviewPermission

  fun isCompletePossible(status: DayCardStatusEnum): Boolean = status === OPEN

  fun isApprovePossible(status: DayCardStatusEnum): Boolean = status === DONE || status === OPEN

  fun isResetPossible(status: DayCardStatusEnum): Boolean = status !== OPEN

  fun isEditPossible(status: DayCardStatusEnum): Boolean = status === OPEN

  @Suppress("SwallowedException")
  fun isDeletePossible(status: DayCardStatusEnum): Boolean {
    try {
      validateDeleteDayCardPossible(status)
    } catch (ex: PreconditionViolationException) {
      return false
    }
    return true
  }

  fun isReschedulePossible(status: DayCardStatusEnum): Boolean = status === OPEN

  fun validateDeleteDayCardPossible(status: DayCardStatusEnum) {
    if (status !== OPEN) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
    }
  }
}
