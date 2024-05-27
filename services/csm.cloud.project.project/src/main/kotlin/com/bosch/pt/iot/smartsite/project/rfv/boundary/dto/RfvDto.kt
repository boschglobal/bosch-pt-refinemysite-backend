/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.boundary.dto

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum

data class RfvDto(val key: DayCardReasonEnum, val active: Boolean, val name: String? = null)
