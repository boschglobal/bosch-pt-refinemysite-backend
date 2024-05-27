/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardDto
import org.springframework.stereotype.Component

@Component
open class DayCardBatchResourceFactory(
    private val dayCardResourceFactoryHelper: DayCardResourceFactoryHelper
) {

  open fun build(dayCards: List<DayCardDto>): BatchResponseResource<DayCardResource> =
      BatchResponseResource(dayCardResourceFactoryHelper.buildFromDtos(dayCards))
}
