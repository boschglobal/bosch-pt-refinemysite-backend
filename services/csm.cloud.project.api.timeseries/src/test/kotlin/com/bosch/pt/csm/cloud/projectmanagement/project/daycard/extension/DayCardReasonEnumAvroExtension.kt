/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro

fun DayCardReasonNotDoneEnumAvro.asReason() = DayCardReasonEnum.valueOf(this.name)
