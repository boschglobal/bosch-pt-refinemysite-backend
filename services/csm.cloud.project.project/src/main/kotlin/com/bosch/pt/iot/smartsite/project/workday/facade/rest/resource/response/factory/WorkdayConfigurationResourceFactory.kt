/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.WorkdayConfigurationResource
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import org.springframework.stereotype.Component

@Component
open class WorkdayConfigurationResourceFactory(
    private val workdayConfigurationResourceFactoryHelper: WorkdayConfigurationResourceFactoryHelper
) {
  open fun build(workdayConfiguration: WorkdayConfiguration): WorkdayConfigurationResource =
      workdayConfigurationResourceFactoryHelper.build(setOf(workdayConfiguration)).first()
}
