/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size

data class SaveDayCardResource(
    @field:Size(min = 1, max = DayCard.MAX_TITLE_LENGTH) val title: String,
    @field:DecimalMin(DayCard.MIN_MANPOWER)
    @field:DecimalMax(DayCard.MAX_MANPOWER)
    val manpower: BigDecimal,
    @field:Size(max = DayCard.MAX_NOTES_LENGTH) val notes: String?,
    val date: LocalDate
)
