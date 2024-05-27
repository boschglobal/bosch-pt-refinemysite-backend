/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import java.time.LocalDate

class TaskScheduleSlotDto(val id: DayCardId, var date: LocalDate) {

  companion object {
    fun convertToMap(slots: List<TaskScheduleSlotDto>?): Map<DayCardId, LocalDate> =
        slots?.associate { it.id to it.date } ?: emptyMap()
  }
}
