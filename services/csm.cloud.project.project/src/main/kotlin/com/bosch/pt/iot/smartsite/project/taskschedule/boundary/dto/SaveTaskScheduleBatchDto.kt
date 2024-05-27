/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.time.LocalDate

class SaveTaskScheduleBatchDto(
    val identifier: TaskScheduleId?,
    val version: Long?,
    val taskIdentifier: TaskId,
    start: LocalDate?,
    end: LocalDate?,
    slots: Map<DayCardId, LocalDate>?
) : SaveTaskScheduleDto(start, end, slots)
