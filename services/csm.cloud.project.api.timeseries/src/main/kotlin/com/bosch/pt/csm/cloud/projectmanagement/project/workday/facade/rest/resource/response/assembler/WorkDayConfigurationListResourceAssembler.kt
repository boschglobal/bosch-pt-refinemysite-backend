/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.WorkDayConfigurationListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import org.springframework.stereotype.Component

@Component
class WorkDayConfigurationListResourceAssembler(
    private val workDayConfigurationResourceAssembler: WorkDayConfigurationResourceAssembler
) {

  fun assemble(
      workDayConfigurations: List<WorkDayConfiguration>,
      latestOnly: Boolean
  ): WorkDayConfigurationListResource =
      WorkDayConfigurationListResource(
          workDayConfigurations
              .flatMap { workDayConfigurationResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
