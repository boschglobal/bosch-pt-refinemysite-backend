/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.WorkDayConfigurationResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import org.springframework.stereotype.Component

@Component
class WorkDayConfigurationResourceAssembler {

  fun assemble(
      workDayConfiguration: WorkDayConfiguration,
      latestOnly: Boolean
  ): List<WorkDayConfigurationResource> =
      if (latestOnly) {
        val workDayConfigurationVersion = workDayConfiguration.history.last()
        listOf(
            WorkDayConfigurationResourceMapper.INSTANCE.fromWorkDayConfigurationVersion(
                workDayConfigurationVersion,
                workDayConfiguration.project,
                workDayConfiguration.identifier,
                workDayConfigurationVersion.workingDays.map { it.key }))
      } else {
        workDayConfiguration.history.map {
          WorkDayConfigurationResourceMapper.INSTANCE.fromWorkDayConfigurationVersion(
              it,
              workDayConfiguration.project,
              workDayConfiguration.identifier,
              it.workingDays.map { it.key })
        }
      }
}
