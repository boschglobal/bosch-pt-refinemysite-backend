/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request

import java.math.BigDecimal
import java.time.LocalDate

class CreateDayCardResource(
    val title: String? = null,
    val manpower: BigDecimal? = null,
    val date: LocalDate? = null,
    val notes: String? = null
)
