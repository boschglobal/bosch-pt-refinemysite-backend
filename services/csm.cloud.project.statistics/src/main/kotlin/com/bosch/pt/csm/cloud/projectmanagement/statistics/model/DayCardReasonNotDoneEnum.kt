/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

enum class DayCardReasonNotDoneEnum {
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
  OTHER,
  CUSTOM1,
  CUSTOM2,
  CUSTOM3,
  CUSTOM4;

  fun isCustom() = this == CUSTOM1 || this == CUSTOM2 || this == CUSTOM3 || this == CUSTOM4
}
