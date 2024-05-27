/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.CreateTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.UpdateTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.batch.CreateTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.batch.UpdateTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskRequestBatchDeleteService
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskRequestDeleteService
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.CreateTaskBatchResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.ChangedTasksResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.ChangedTasksResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskListResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.DELETE_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.annotation.Nonnull
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@Validated
@RestController
open class TaskController(
    private val changedTasksResourceFactory: ChangedTasksResourceFactory,
    private val createTaskCommandHandler: CreateTaskCommandHandler,
    private val updateTaskCommandHandler: UpdateTaskCommandHandler,
    private val createTaskBatchCommandHandler: CreateTaskBatchCommandHandler,
    private val updateTaskBatchCommandHandler: UpdateTaskBatchCommandHandler,
    private val taskRequestDeleteService: TaskRequestDeleteService,
    private val taskRequestBatchDeleteService: TaskRequestBatchDeleteService,
    private val taskQueryService: TaskQueryService,
    private val taskResourceFactory: TaskResourceFactory,
    private val taskListResourceFactory: TaskListResourceFactory,
    private val taskBatchResourceFactory: TaskBatchResourceFactory
) {

  @PostMapping(TASKS_ENDPOINT, TASK_BY_TASK_ID_ENDPOINT)
  open fun createTask(
      @PathVariable(value = PATH_VARIABLE_TASK_ID, required = false) taskIdentifier: TaskId?,
      @RequestBody @Valid saveTaskResource: SaveTaskResource
  ): ResponseEntity<TaskResource> {
    val createdTaskIdentifier =
        createTaskCommandHandler.handle(
            CreateTaskCommand(
                identifier = taskIdentifier ?: TaskId(),
                projectIdentifier = saveTaskResource.projectId,
                name = saveTaskResource.name,
                description = saveTaskResource.description,
                location = saveTaskResource.location,
                projectCraftIdentifier = saveTaskResource.projectCraftId,
                assigneeIdentifier = saveTaskResource.assigneeId,
                workAreaIdentifier = saveTaskResource.workAreaId,
                status = saveTaskResource.status))

    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(TASK_BY_TASK_ID_ENDPOINT)
            .buildAndExpand(createdTaskIdentifier)
            .toUri()

    return taskQueryService.findTaskWithDetails(createdTaskIdentifier).let {
      ResponseEntity.created(location).eTag(it.toEtagString()).body(taskResourceFactory.build(it))
    }
  }

  @PostMapping(TASKS_BATCH_ENDPOINT)
  open fun createTasks(
      @RequestBody
      @Valid
      createBatchRequestResource: CreateBatchRequestResource<CreateTaskBatchResource>
  ): ResponseEntity<ChangedTasksResource> {
    val createdTaskIdentifiers =
        createTaskBatchCommandHandler.handle(
            createBatchRequestResource.items.map {
              CreateTaskCommand(
                  identifier = it.id?.asTaskId() ?: TaskId(),
                  projectIdentifier = it.projectId,
                  name = it.name,
                  description = it.description,
                  location = it.location,
                  projectCraftIdentifier = it.projectCraftId,
                  assigneeIdentifier = it.assigneeId,
                  workAreaIdentifier = it.workAreaId,
                  status = it.status)
            })

    return taskQueryService.findTasksWithDetails(createdTaskIdentifiers).let {
      ResponseEntity.ok().body(changedTasksResourceFactory.build(it, true))
    }
  }

  @GetMapping(TASK_BY_TASK_ID_ENDPOINT)
  open fun findOneById(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskResource> {
    val task = taskQueryService.findTaskWithDetails(taskIdentifier)
    return ResponseEntity.ok().eTag(task.toEtagString()).body(taskResourceFactory.build(task))
  }

  @GetMapping(TASKS_BY_PROJECT_ID_ENDPOINT)
  open fun findAll(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestParam(defaultValue = "false", required = false) showBasedOnRole: Boolean?,
      @PageableDefault(
          sort = ["name", "status"],
          direction = Sort.Direction.ASC,
          size = PageableDefaults.DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<TaskListResource> {
    val tasks = taskQueryService.findTasks(projectIdentifier, showBasedOnRole, pageable)
    return ResponseEntity(
        taskListResourceFactory.build(tasks, pageable, projectIdentifier, showBasedOnRole),
        HttpStatus.OK)
  }

  @PostMapping(TaskControllerUtils.FIND_BATCH_ENDPOINT)
  open fun findBatch(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid batchRequestResource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<TaskResource>> {

    val tasks = taskQueryService.findBatch(batchRequestResource.ids.asTaskIds(), projectId)

    return ResponseEntity.ok().body(taskBatchResourceFactory.build(tasks, projectId))
  }

  @PutMapping(TASK_BY_TASK_ID_ENDPOINT)
  open fun updateTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @Nonnull @RequestBody @Valid saveTaskResource: SaveTaskResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") @Nonnull eTag: ETag
  ): ResponseEntity<TaskResource> {
    updateTaskCommandHandler.handle(
        UpdateTaskCommand(
            taskIdentifier,
            saveTaskResource.projectId,
            eTag.toVersion(),
            saveTaskResource.name,
            saveTaskResource.description,
            saveTaskResource.location,
            saveTaskResource.projectCraftId,
            saveTaskResource.workAreaId,
            saveTaskResource.assigneeId,
            saveTaskResource.status))

    return taskQueryService.findTaskWithDetails(taskIdentifier).let {
      ResponseEntity.ok().eTag(it.toEtagString()).body(taskResourceFactory.build(it))
    }
  }

  @PutMapping(TASKS_BATCH_ENDPOINT)
  open fun updateTasks(
      @RequestBody
      @Nonnull
      @Valid
      updateBatchResources: UpdateBatchRequestResource<SaveTaskResourceWithIdentifierAndVersion>
  ): ResponseEntity<ChangedTasksResource> {
    val taskIdentifiers = updateBatchResources.items.map { it.id.asTaskId() }

    updateTaskBatchCommandHandler.handle(
        updateBatchResources.items.map {
          UpdateTaskCommand(
              it.id.asTaskId(),
              it.projectId,
              it.version,
              it.name,
              it.description,
              it.location,
              it.projectCraftId,
              it.workAreaId,
              it.assigneeId,
              it.status)
        })

    return taskQueryService.findTasksWithDetails(taskIdentifiers).let {
      ResponseEntity.ok().body(changedTasksResourceFactory.build(it, true))
    }
  }

  @DeleteMapping(TASK_BY_TASK_ID_ENDPOINT)
  open fun deleteTask(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<Void> {
    taskRequestDeleteService.markAsDeletedAndSendEvent(taskIdentifier)
    return ResponseEntity.noContent().build()
  }

  @DeleteMapping(DELETE_TASKS_BATCH_ENDPOINT)
  open fun deleteTasks(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<Void> {
    taskRequestBatchDeleteService.markAsDeletedAndSendEvents(resource.ids.map { it.asTaskId() })
    return ResponseEntity.noContent().build()
  }
}
