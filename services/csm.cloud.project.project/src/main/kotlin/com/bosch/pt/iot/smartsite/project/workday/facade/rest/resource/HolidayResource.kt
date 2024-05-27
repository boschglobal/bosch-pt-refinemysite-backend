/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource

import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday.Companion.MAX_NAME_LENGTH
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class HolidayResource(
    @field:Size(min = 1, max = MAX_NAME_LENGTH) val name: String,
    val date: LocalDate
)
