/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import java.time.LocalDate

open class SaveTaskScheduleDto(
    val start: LocalDate?,
    val end: LocalDate?,
    val slots: Map<DayCardId, LocalDate>?
)
