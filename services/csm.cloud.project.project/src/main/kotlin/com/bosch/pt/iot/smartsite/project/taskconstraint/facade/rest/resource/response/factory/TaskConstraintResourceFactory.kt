/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.TaskConstraintDto
import org.springframework.stereotype.Component

@Component
open class TaskConstraintResourceFactory(
    private val factoryHelper: TaskConstraintResourceFactoryHelper
) {

  open fun build(projectIdentifier: ProjectId, constraint: TaskConstraintDto) =
      factoryHelper.build(projectIdentifier, listOf(constraint)).first()
}
