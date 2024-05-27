/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import org.springframework.stereotype.Component

@Component
open class TaskConstraintSelectionListResourceFactory(
    private val factoryHelper: TaskConstraintSelectionResourceFactoryHelper
) {

  open fun build(
      projectIdentifier: ProjectId,
      constraintSelections: List<TaskConstraintSelectionDto>
  ) =
      TaskConstraintSelectionListResource(
          factoryHelper.build(projectIdentifier, constraintSelections))
}
