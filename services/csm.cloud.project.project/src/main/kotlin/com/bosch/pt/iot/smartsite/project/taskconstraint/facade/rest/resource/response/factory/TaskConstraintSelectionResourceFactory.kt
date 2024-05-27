/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import org.springframework.stereotype.Component

@Component
open class TaskConstraintSelectionResourceFactory(
    private val factoryHelper: TaskConstraintSelectionResourceFactoryHelper
) {

  open fun build(
      projectIdentifier: ProjectId,
      taskIdentifier: TaskId,
      constraintSelection: TaskConstraintSelectionDto?
  ) =
      factoryHelper
          .build(
              projectIdentifier,
              listOf(
                  constraintSelection
                      ?: TaskConstraintSelectionDto(
                          taskIdentifier.toUuid(), 0L, taskIdentifier, emptyList())))
          .first()
}
