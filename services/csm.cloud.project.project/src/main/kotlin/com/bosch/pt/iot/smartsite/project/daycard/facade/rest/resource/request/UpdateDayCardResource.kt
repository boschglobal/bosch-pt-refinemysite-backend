/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard.Companion.MAX_MANPOWER
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard.Companion.MAX_NOTES_LENGTH
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard.Companion.MAX_TITLE_LENGTH
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard.Companion.MIN_MANPOWER
import java.math.BigDecimal
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size

data class UpdateDayCardResource(
    // Title
    @field:Size(min = 1, max = MAX_TITLE_LENGTH) val title: String,

    // Man power
    @field:DecimalMin(MIN_MANPOWER) @field:DecimalMax(MAX_MANPOWER) val manpower: BigDecimal,

    // Notes
    @field:Size(max = MAX_NOTES_LENGTH) val notes: String?
)
