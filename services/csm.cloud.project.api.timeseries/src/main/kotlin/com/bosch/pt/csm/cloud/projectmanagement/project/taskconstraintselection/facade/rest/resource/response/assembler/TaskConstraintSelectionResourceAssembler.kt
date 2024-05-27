/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.ProjectTaskConstraintSelections
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelection
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelectionVersion
import org.springframework.stereotype.Component

@Component
class TaskConstraintSelectionResourceAssembler {

  fun assembleLatest(
      constraintSelections: ProjectTaskConstraintSelections
  ): List<TaskConstraintSelectionResource> =
      constraintSelections.constraints.flatMap { constraint ->
        constraint.history.last().let { version ->
          assembleResource(constraintSelections, version, constraint)
        }
      }

  fun assemble(
      constraintSelections: ProjectTaskConstraintSelections,
  ): List<TaskConstraintSelectionResource> =
      constraintSelections.constraints.flatMap { constraint ->
        constraint.history.flatMap { version ->
          assembleResource(constraintSelections, version, constraint)
        }
      }

  private fun assembleResource(
      constraintSelections: ProjectTaskConstraintSelections,
      version: TaskConstraintSelectionVersion,
      constraint: TaskConstraintSelection
  ): List<TaskConstraintSelectionResource> =
      version.constraints.map { constraintKey ->
        TaskConstraintSelectionResourceMapper.INSTANCE.fromTaskConstraintVersion(
            version,
            constraintSelections.projectId,
            constraintSelections.taskId,
            constraint.identifier,
            constraintKey.key,
            version.eventDate)
      }
}
