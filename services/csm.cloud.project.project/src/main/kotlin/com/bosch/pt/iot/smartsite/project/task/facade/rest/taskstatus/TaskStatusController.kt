/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskstatus

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.AcceptTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.CloseTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.ResetTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.SendTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.StartTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.AcceptTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.CloseTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.ResetTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.SendTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.StartTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ACCEPT_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ACCEPT_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.CLOSE_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.CLOSE_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.RESET_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.RESET_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.SEND_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.SEND_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.START_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.START_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@Validated
@RestController
open class TaskStatusController(
    private val acceptTaskCommandHandler: AcceptTaskCommandHandler,
    private val acceptTaskBatchCommandHandler: AcceptTaskBatchCommandHandler,
    private val closeTaskCommandHandler: CloseTaskCommandHandler,
    private val closeTaskBatchCommandHandler: CloseTaskBatchCommandHandler,
    private val resetTaskCommandHandler: ResetTaskCommandHandler,
    private val resetTaskBatchCommandHandler: ResetTaskBatchCommandHandler,
    private val sendTaskBatchCommandHandler: SendTaskBatchCommandHandler,
    private val sendTaskCommandHandler: SendTaskCommandHandler,
    private val startTaskCommandHandler: StartTaskCommandHandler,
    private val startTaskBatchCommandHandler: StartTaskBatchCommandHandler,
    private val taskQueryService: TaskQueryService,
    private val taskResourceFactory: TaskResourceFactory,
    private val taskBatchResourceFactory: TaskBatchResourceFactory
) {

  @PostMapping(START_TASK_BY_TASK_ID_ENDPOINT)
  open fun startTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskResource> {
    startTaskCommandHandler.handle(taskIdentifier)

    val task = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ok().eTag(task.toEtagString()).body(taskResourceFactory.build(task))
  }

  @PostMapping(START_TASKS_BATCH_ENDPOINT)
  open fun startTasks(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<TaskResource>> {
    startTaskBatchCommandHandler.handle(projectId, resource.ids.map { it.asTaskId() })

    val tasks = taskQueryService.findTasksWithDetails(resource.ids.map { it.asTaskId() })
    return ok().body(taskBatchResourceFactory.build(tasks, projectId))
  }

  @PostMapping(CLOSE_TASK_BY_TASK_ID_ENDPOINT)
  open fun closeTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskResource> {
    closeTaskCommandHandler.handle(taskIdentifier)

    val task = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ok().eTag(task.toEtagString()).body(taskResourceFactory.build(task))
  }

  @PostMapping(CLOSE_TASKS_BATCH_ENDPOINT)
  open fun closeTasks(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<TaskResource>> {
    closeTaskBatchCommandHandler.handle(projectId, resource.ids.map { it.asTaskId() })

    val tasks = taskQueryService.findTasksWithDetails(resource.ids.map { it.asTaskId() })
    return ok().body(taskBatchResourceFactory.build(tasks, projectId))
  }

  @PostMapping(ACCEPT_TASK_BY_TASK_ID_ENDPOINT)
  open fun acceptTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskResource> {
    acceptTaskCommandHandler.handle(taskIdentifier)

    val task = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ok().eTag(task.toEtagString()).body(taskResourceFactory.build(task))
  }

  @PostMapping(ACCEPT_TASKS_BATCH_ENDPOINT)
  open fun acceptTasks(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<TaskResource>> {
    acceptTaskBatchCommandHandler.handle(projectId, resource.ids.map { it.asTaskId() })

    val tasks = taskQueryService.findTasksWithDetails(resource.ids.map { it.asTaskId() })
    return ok().body(taskBatchResourceFactory.build(tasks, projectId))
  }

  @PostMapping(SEND_TASK_BY_TASK_ID_ENDPOINT)
  open fun sendTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskResource> {
    sendTaskCommandHandler.handle(taskIdentifier)

    val task = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ok().eTag(task.toEtagString()).body(taskResourceFactory.build(task))
  }

  @PostMapping(SEND_TASKS_BATCH_ENDPOINT)
  open fun sendTasks(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<TaskResource>> {
    sendTaskBatchCommandHandler.handle(projectId, resource.ids.map { it.asTaskId() })

    val tasks = taskQueryService.findTasksWithDetails(resource.ids.map { it.asTaskId() })
    return ok().body(taskBatchResourceFactory.build(tasks, projectId))
  }

  @PostMapping(RESET_TASK_BY_TASK_ID_ENDPOINT)
  open fun resetTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskResource> {
    resetTaskCommandHandler.handle(taskIdentifier)
    val task = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ok().eTag(task.toEtagString()).body(taskResourceFactory.build(task))
  }

  @PostMapping(RESET_TASKS_BATCH_ENDPOINT)
  open fun resetTasks(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<TaskResource>> {
    resetTaskBatchCommandHandler.handle(projectId, resource.ids.map { it.asTaskId() })

    val tasks = taskQueryService.findTasksWithDetails(resource.ids.map { it.asTaskId() })
    return ok().body(taskBatchResourceFactory.build(tasks, projectId))
  }
}
