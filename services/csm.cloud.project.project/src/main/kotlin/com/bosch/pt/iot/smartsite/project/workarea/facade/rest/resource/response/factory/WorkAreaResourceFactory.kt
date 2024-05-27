/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
open class WorkAreaResourceFactory(
    messageSource: MessageSource,
    private val workAreaResourceFactoryHelper: WorkAreaResourceFactoryHelper
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(workArea: WorkArea, project: Project): WorkAreaResource =
      workAreaResourceFactoryHelper.build(listOf(workArea), project).first()
}
