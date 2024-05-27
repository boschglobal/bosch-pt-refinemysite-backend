/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.UpdateTaskConstraintDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import jakarta.validation.constraints.Size

data class UpdateTaskConstraintResource(

    // reason
    val key: TaskConstraintEnum,

    // active
    val active: Boolean,

    // optional name
    @field:Size(min = 1, max = TaskConstraintCustomization.MAX_NAME_LENGTH) val name: String? = null
) {

  fun toDto(projectIdentifier: ProjectId) =
      UpdateTaskConstraintDto(projectIdentifier, key, active, name)
}
