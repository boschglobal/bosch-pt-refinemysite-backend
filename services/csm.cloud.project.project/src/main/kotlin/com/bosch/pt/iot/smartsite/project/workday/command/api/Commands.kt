/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.api

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import java.time.DayOfWeek

data class CreateWorkdayConfigurationCommand(val projectRef: ProjectId)

data class UpdateWorkdayConfigurationCommand(
    val version: Long,
    val projectRef: ProjectId,
    val startOfWeek: DayOfWeek,
    val workingDays: List<DayOfWeek>,
    val holidays: List<Holiday>,
    val allowWorkOnNonWorkingDays: Boolean
)
