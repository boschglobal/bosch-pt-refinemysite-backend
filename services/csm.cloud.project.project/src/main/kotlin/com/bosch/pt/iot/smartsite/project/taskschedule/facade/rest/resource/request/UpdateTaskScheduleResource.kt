/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request

import java.time.LocalDate
import jakarta.validation.Valid

open class UpdateTaskScheduleResource(
    override val start: LocalDate?,
    override val end: LocalDate?,
    @field:Valid open val slots: List<TaskScheduleSlotDto>?
) : CreateTaskScheduleResource(start, end)
