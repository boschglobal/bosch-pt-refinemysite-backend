/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.TaskConstraintPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.assembler.TaskConstraintPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.ProjectTaskConstraints
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.service.TaskConstraintQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.TaskConstraintSelectionPayloadV1
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class TaskConstraintGraphQlController(
    private val taskConstraintPayloadAssembler: TaskConstraintPayloadAssembler,
    private val taskConstraintQueryService: TaskConstraintQueryService
) {

  @BatchMapping("constraints")
  fun constraintsInProjects(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, List<TaskConstraintPayloadV1>> {
    val constraints =
        taskConstraintQueryService.findAllByProjectsAndDeletedFalse(
            projects.map { it.id.asProjectId() })

    return projects.associateWith { project ->
      project.id.asProjectId().let {
        taskConstraintPayloadAssembler.assembleAddMissing(
            ProjectTaskConstraints(it, constraints[it] ?: emptyList()))
      }
    }
  }

  @BatchMapping("items")
  fun constraintsInTaskSelection(
      constraintSelections: List<TaskConstraintSelectionPayloadV1>
  ): Map<TaskConstraintSelectionPayloadV1, List<TaskConstraintPayloadV1>> {
    val constraints =
        taskConstraintQueryService.findAllByProjectsAndDeletedFalse(
            constraintSelections.map { it.id.asProjectId() }.distinct())

    return constraintSelections.associateWith { project ->
      val projectId = project.id.asProjectId()
      constraints[projectId].let {
        taskConstraintPayloadAssembler.assemble(
            ProjectTaskConstraints(projectId, it ?: emptyList()), project.constraintIds)
      }
    }
  }
}
