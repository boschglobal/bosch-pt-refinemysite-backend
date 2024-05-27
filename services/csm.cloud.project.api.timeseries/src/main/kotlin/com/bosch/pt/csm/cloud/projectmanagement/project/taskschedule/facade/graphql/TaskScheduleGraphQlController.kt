/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response.TaskSchedulePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response.assembler.TaskSchedulePayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service.TaskScheduleQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class TaskScheduleGraphQlController(
    private val taskSchedulePayloadAssembler: TaskSchedulePayloadAssembler,
    private val taskScheduleQueryService: TaskScheduleQueryService
) {

  @BatchMapping
  fun schedule(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, TaskSchedulePayloadV1?> {
    val taskSchedules =
        taskScheduleQueryService
            .findAllByTasksAndDeletedFalse(tasks.map { it.id.asTaskId() })
            .associateBy { it.task }

    return tasks.associateWith { task ->
      taskSchedules[task.id.asTaskId()]?.let { taskSchedulePayloadAssembler.assemble(it) }
    }
  }
}
