/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import org.springframework.stereotype.Component

@Component
class TaskListResourceAssembler(private val taskResourceAssembler: TaskResourceAssembler) {

  fun assemble(
      tasks: List<Task>,
      schedules: Map<TaskId, List<TaskSchedule>>,
      latestOnly: Boolean
  ): TaskListResource =
      if (latestOnly) {
        TaskListResource(
            tasks
                .flatMap {
                  taskResourceAssembler.assembleLatest(it, schedules[it.identifier] ?: emptyList())
                }
                .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
      } else {
        TaskListResource(
            tasks
                .flatMap {
                  taskResourceAssembler.assemble(it, schedules[it.identifier] ?: emptyList())
                }
                .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
      }
}
