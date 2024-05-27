/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import java.util.Collections.singletonList
import org.springframework.stereotype.Component

@Component
open class MilestoneResourceFactory(
    private val resourceFactoryHelper: MilestoneResourceFactoryHelper
) {

  open fun build(milestone: Milestone): MilestoneResource =
      resourceFactoryHelper.build(singletonList(milestone)).first()
}
