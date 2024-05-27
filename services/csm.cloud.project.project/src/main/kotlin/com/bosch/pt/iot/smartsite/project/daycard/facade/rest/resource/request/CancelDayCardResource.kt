/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum

data class CancelDayCardResource(val reason: DayCardReasonEnum)
