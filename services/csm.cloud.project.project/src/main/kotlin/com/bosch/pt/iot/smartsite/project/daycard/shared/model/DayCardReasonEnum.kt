/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.model

enum class DayCardReasonEnum {
  DELAYED_MATERIAL,
  NO_CONCESSION,
  CONCESSION_NOT_RECOGNIZED,
  CHANGED_PRIORITY,
  MANPOWER_SHORTAGE,
  OVERESTIMATION,
  TOUCHUP,
  MISSING_INFOS,
  MISSING_TOOLS,
  BAD_WEATHER,
  CUSTOM1,
  CUSTOM2,
  CUSTOM3,
  CUSTOM4;

  val isCustom: Boolean
    get() = this == CUSTOM1 || this == CUSTOM2 || this == CUSTOM3 || this == CUSTOM4

  val isStandard: Boolean
    get() = !isCustom

  companion object {
    fun getStandardRfvs(): List<DayCardReasonEnum> = values().filter { it.isStandard }

    fun getCustomRfvs(): List<DayCardReasonEnum> = values().filter { it.isCustom }
  }
}
