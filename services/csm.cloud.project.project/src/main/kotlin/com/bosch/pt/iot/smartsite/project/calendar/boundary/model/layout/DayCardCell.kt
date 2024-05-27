/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.APPROVED
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.DONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.NOTDONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN

data class DayCardCell(
    val name: String,
    val manpower: String,
    val status: DayCardStatusEnum,
    val type: DayCardCellType
) {

  fun getStatusIcon() =
      when (status) {
        OPEN -> "#day-card-status-open"
        DONE -> "#day-card-status-done"
        APPROVED -> "#day-card-status-approved"
        NOTDONE -> "#day-card-status-not-done"
      }
}

enum class DayCardCellType {
  DAYCARD,
  BLANK,
  OUT_OF_SCHEDULE;

  fun isDayCard() = this == DAYCARD

  fun isBlank() = this == BLANK

  fun isOutOfSchedule() = this == OUT_OF_SCHEDULE
}
