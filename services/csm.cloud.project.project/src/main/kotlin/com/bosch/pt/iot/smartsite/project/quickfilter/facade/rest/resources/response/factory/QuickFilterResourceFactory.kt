/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.factory

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import org.springframework.stereotype.Component

@Component
open class QuickFilterResourceFactory(
    private val quickFilterResourceFactoryHelper: QuickFilterResourceFactoryHelper
) {

  open fun build(quickFilter: QuickFilter, projectRef: ProjectId): QuickFilterResource =
      quickFilterResourceFactoryHelper.build(listOf(quickFilter), projectRef).first()
}
