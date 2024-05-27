/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.assembler.TaskListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.service.TaskQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service.TaskScheduleQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class TaskRestController(
    private val participantQueryService: ParticipantQueryService,
    private val taskListResourceAssembler: TaskListResourceAssembler,
    private val taskScheduleQueryService: TaskScheduleQueryService,
    private val taskQueryService: TaskQueryService
) {

  companion object {
    const val TASK_ENDPOINT = "/projects/tasks"
  }

  @GetMapping(TASK_ENDPOINT)
  fun findTasks(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<TaskListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val tasks: List<Task>
    val schedules: Map<TaskId, List<TaskSchedule>>
    if (latestOnly) {
      tasks = taskQueryService.findAllByProjectsAndDeletedFalse(projectIds)
      schedules =
          taskScheduleQueryService
              .findAllByTasksAndDeletedFalse(tasks.map { it.identifier })
              .groupBy { it.task }
    } else {
      tasks = taskQueryService.findAllByProjects(projectIds)
      schedules =
          taskScheduleQueryService.findAllByTasks(tasks.map { it.identifier }).groupBy { it.task }
    }

    return ResponseEntity.ok()
        .body(taskListResourceAssembler.assemble(tasks, schedules, latestOnly))
  }
}
