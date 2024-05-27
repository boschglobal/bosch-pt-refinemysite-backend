/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.DayEnum
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro

fun DayEnumAvro.asDay() = DayEnum.valueOf(this.name)
