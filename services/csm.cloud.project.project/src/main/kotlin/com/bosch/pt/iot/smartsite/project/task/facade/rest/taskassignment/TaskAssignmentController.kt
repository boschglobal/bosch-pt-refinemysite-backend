/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskassignment

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskAssignmentCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.UnassignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.batch.AssignTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.batch.UnassignTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.SendTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.SendTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.AssignTaskListToParticipantResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.AssignTaskToParticipantResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.ChangedTasksResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.ChangedTasksResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ASSIGN_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ASSIGN_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.UNASSIGN_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.UNASSIGN_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@Validated
@RestController
open class TaskAssignmentController(
    private val assignTaskBatchCommandHandler: AssignTaskBatchCommandHandler,
    private val assignTaskCommandHandler: AssignTaskCommandHandler,
    private val changedTasksResourceFactory: ChangedTasksResourceFactory,
    private val sendTaskBatchCommandHandler: SendTaskBatchCommandHandler,
    private val sendTaskCommandHandler: SendTaskCommandHandler,
    private val taskQueryService: TaskQueryService,
    private val taskResourceFactory: TaskResourceFactory,
    private val unassignTaskBatchCommandHandler: UnassignTaskBatchCommandHandler,
    private val unassignTaskCommandHandler: UnassignTaskCommandHandler,
) {

  @PostMapping(ASSIGN_TASK_BY_TASK_ID_ENDPOINT)
  open fun assignTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @RequestBody @Valid assignTask: AssignTaskToParticipantResource
  ): ResponseEntity<TaskResource> {

    assignTaskCommandHandler.handle(taskIdentifier, assignTask.assigneeId)

    // In case the task is in DRAFT status up to now, it will be automatically changed to OPEN.
    // This is dubbed "sending" in the user world.
    val task = taskQueryService.findTask(taskIdentifier)
    if (task.status == DRAFT) {
      sendTaskCommandHandler.handle(taskIdentifier)
    }

    val taskWithDetails = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ResponseEntity.ok()
        .eTag(taskWithDetails.toEtagString())
        .body(taskResourceFactory.build(taskWithDetails))
  }

  @PostMapping(ASSIGN_TASKS_BATCH_ENDPOINT)
  open fun assignTasks(
      @RequestBody @Valid resource: AssignTaskListToParticipantResource
  ): ResponseEntity<ChangedTasksResource> {
    val taskIdentifiers = resource.taskIds.map { it.asTaskId() }

    val projectId =
        taskQueryService.findProjectIdentifierByIdentifier(resource.taskIds.first().asTaskId())

    assignTaskBatchCommandHandler.handle(
        projectId,
        resource.taskIds.map { UpdateTaskAssignmentCommand(it.asTaskId(), resource.assigneeId) })

    // A task can be "sent" only once, so check its status in order to not send it again
    val tasks = taskQueryService.findTasks(taskIdentifiers).filter { it.status == DRAFT }

    tasks.map { it.identifier }.let { sendTaskBatchCommandHandler.handle(projectId, it) }

    val tasksWithDetails = taskQueryService.findTasksWithDetails(taskIdentifiers)
    return ResponseEntity(changedTasksResourceFactory.build(tasksWithDetails, false), OK)
  }

  @PostMapping(UNASSIGN_TASK_BY_TASK_ID_ENDPOINT)
  open fun unassignTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
  ): ResponseEntity<TaskResource> {
    unassignTaskCommandHandler.handle(taskIdentifier)

    return taskQueryService.findTaskWithDetails(taskIdentifier).let {
      ResponseEntity.ok().eTag(it.toEtagString()).body(taskResourceFactory.build(it))
    }
  }

  @PostMapping(UNASSIGN_TASKS_BATCH_ENDPOINT)
  open fun unassignTasks(
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<ChangedTasksResource> {
    val taskIdentifiers = resource.ids.asTaskIds().toList()
    val projectIdentifier =
        taskQueryService.findProjectIdentifierByIdentifier(
            taskIdentifiers.first().identifier.asTaskId())

    unassignTaskBatchCommandHandler.handle(projectIdentifier, taskIdentifiers)

    return ResponseEntity(
        changedTasksResourceFactory.build(
            taskQueryService.findTasksWithDetails(taskIdentifiers), false),
        OK)
  }
}
