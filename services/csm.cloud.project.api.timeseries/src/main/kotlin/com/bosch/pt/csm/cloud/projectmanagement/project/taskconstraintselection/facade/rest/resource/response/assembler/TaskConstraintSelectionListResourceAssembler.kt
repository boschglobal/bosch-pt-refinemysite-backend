/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.ProjectTaskConstraintSelections
import org.springframework.stereotype.Component

@Component
class TaskConstraintSelectionListResourceAssembler(
    private val taskConstraintSelectionResourceAssembler: TaskConstraintSelectionResourceAssembler
) {

  fun assemble(
      constraints: List<ProjectTaskConstraintSelections>,
      latestOnly: Boolean
  ): TaskConstraintSelectionListResource =
      if (latestOnly) {
        TaskConstraintSelectionListResource(
            constraints
                .flatMap { taskConstraintSelectionResourceAssembler.assembleLatest(it) }
                .sortedWith(
                    compareBy({ it.id.value }, { it.version }, { it.key }, { it.eventTimestamp })))
      } else {
        TaskConstraintSelectionListResource(
            constraints
                .flatMap { taskConstraintSelectionResourceAssembler.assemble(it) }
                .sortedWith(
                    compareBy({ it.id.value }, { it.version }, { it.key }, { it.eventTimestamp })))
      }
}
