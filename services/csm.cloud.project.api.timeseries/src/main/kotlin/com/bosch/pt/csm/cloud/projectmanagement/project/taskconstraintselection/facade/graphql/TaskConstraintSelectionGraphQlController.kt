/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.TaskConstraintSelectionPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.assembler.TaskConstraintSelectionPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.service.TaskConstraintSelectionQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class TaskConstraintSelectionGraphQlController(
    private val taskConstraintSelectionPayloadAssembler: TaskConstraintSelectionPayloadAssembler,
    private val taskConstraintSelectionQueryService: TaskConstraintSelectionQueryService
) {

  @BatchMapping("constraints")
  fun constraintsInTask(
      tasks: List<TaskPayloadV1>
  ): Map<TaskPayloadV1, TaskConstraintSelectionPayloadV1?> {
    val constraints =
        taskConstraintSelectionQueryService.findAllByTasksAndDeletedFalse(
            tasks.map { it.id.asTaskId() })

    return tasks.associateWith { task ->
      val taskId = task.id.asTaskId()
      constraints[taskId].let {
        it?.maxByOrNull { it.eventDate }
            ?.let { taskConstraintSelectionPayloadAssembler.assemble(it) }
      }
    }
  }
}
