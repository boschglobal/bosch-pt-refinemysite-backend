/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.HolidayPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.WorkDayConfigurationPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import org.springframework.stereotype.Component

@Component
class WorkDayConfigurationPayloadAssembler {

  fun assemble(workDayConfiguration: WorkDayConfiguration): WorkDayConfigurationPayloadV1 =
      WorkDayConfigurationPayloadMapper.INSTANCE.fromWorkDayConfiguration(
          workDayConfiguration,
          workDayConfiguration.workingDays.map { it.shortKey },
          workDayConfiguration.holidays.map { HolidayPayloadV1(it.name, it.date) })
}
