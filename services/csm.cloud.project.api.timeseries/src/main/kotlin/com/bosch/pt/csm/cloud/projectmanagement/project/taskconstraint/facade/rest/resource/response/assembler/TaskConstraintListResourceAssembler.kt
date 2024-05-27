/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.TaskConstraintListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.ProjectTaskConstraints
import org.springframework.stereotype.Component

@Component
class TaskConstraintListResourceAssembler(
    private val taskConstraintResourceAssembler: TaskConstraintResourceAssembler
) {

  fun assemble(
      constraints: List<ProjectTaskConstraints>,
      latestOnly: Boolean
  ): TaskConstraintListResource =
      if (latestOnly) {
        TaskConstraintListResource(
            constraints
                .flatMap { taskConstraintResourceAssembler.assembleLatest(it) }
                .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
      } else {
        TaskConstraintListResource(
            constraints
                .flatMap { taskConstraintResourceAssembler.assemble(it) }
                .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
      }
}
