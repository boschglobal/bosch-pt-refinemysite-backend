/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import org.springframework.stereotype.Component

@Component
open class DayCardResourceFactory(
    private val dayCardResourceFactoryHelper: DayCardResourceFactoryHelper
) {

  open fun build(dayCard: DayCard): DayCardResource {
    return dayCardResourceFactoryHelper.build(listOf(dayCard)).first()
  }
}
