/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.command.api.UpdateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.HolidayResource
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday.Companion.MAX_HOLIDAY_AMOUNT
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration.Companion.MAX_WORKDAYS_NUMBER
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.DayOfWeek

data class UpdateWorkdayConfigurationResource(
    val startOfWeek: DayOfWeek,
    @field:Size(max = MAX_WORKDAYS_NUMBER) val workingDays: List<DayOfWeek>,
    @field:Valid @field:Size(max = MAX_HOLIDAY_AMOUNT) val holidays: List<HolidayResource>,
    val allowWorkOnNonWorkingDays: Boolean
) {
  fun toCommand(projectId: ProjectId, eTag: ETag) =
      UpdateWorkdayConfigurationCommand(
          version = eTag.toVersion(),
          projectRef = projectId,
          startOfWeek = startOfWeek,
          workingDays = workingDays,
          holidays = holidays.toHolidays(),
          allowWorkOnNonWorkingDays = allowWorkOnNonWorkingDays)

  private fun List<HolidayResource>.toHolidays() = map { Holiday(it.name, it.date) }
}
