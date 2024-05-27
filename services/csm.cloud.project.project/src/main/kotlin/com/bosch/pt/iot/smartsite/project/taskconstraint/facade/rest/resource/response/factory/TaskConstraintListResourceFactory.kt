/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.TaskConstraintDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource
import org.springframework.stereotype.Component

@Component
open class TaskConstraintListResourceFactory(
    private val taskConstraintResourceFactoryHelper: TaskConstraintResourceFactoryHelper
) {

  open fun build(
      projectIdentifier: ProjectId,
      constraints: List<TaskConstraintDto>
  ): BatchResponseResource<TaskConstraintResource> =
      BatchResponseResource(
          taskConstraintResourceFactoryHelper.build(projectIdentifier, constraints))
}
