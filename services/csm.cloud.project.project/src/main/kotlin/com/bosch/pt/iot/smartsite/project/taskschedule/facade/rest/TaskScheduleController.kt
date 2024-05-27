/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK_SCHEDULE
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.DeleteTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.CreateTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.DeleteTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.UpdateTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch.CreateTaskScheduleBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch.UpdateTaskScheduleBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.service.TaskScheduleService
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleIds
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.assembler.SaveTaskScheduleBatchDtoAssembler.assembleOfCreate
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.assembler.SaveTaskScheduleBatchDtoAssembler.assembleOfUpdate
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.assembler.SaveTaskScheduleDtoAssembler.assemble
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleListResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory.TaskScheduleListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory.TaskScheduleResourceFactory
import com.bosch.pt.iot.smartsite.project.taskschedule.query.TaskScheduleQueryService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
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
open class TaskScheduleController(
    private val createTaskScheduleCommandHandler: CreateTaskScheduleCommandHandler,
    private val updateTaskScheduleCommandHandler: UpdateTaskScheduleCommandHandler,
    private val deleteTaskScheduleCommandHandler: DeleteTaskScheduleCommandHandler,
    private val updateTaskScheduleBatchCommandHandler: UpdateTaskScheduleBatchCommandHandler,
    private val createTaskScheduleBatchCommandHandler: CreateTaskScheduleBatchCommandHandler,
    private val taskScheduleQueryService: TaskScheduleQueryService,
    private val taskScheduleService: TaskScheduleService,
    private val taskScheduleListResourceFactory: TaskScheduleListResourceFactory,
    private val taskScheduleResourceFactory: TaskScheduleResourceFactory
) {

  @PostMapping(SCHEDULE_BY_TASK_ID_ENDPOINT)
  open fun createTaskSchedule(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @RequestBody @Valid resource: CreateTaskScheduleResource
  ): ResponseEntity<TaskScheduleResource> {
    val taskScheduleDto = assemble(resource)
    createTaskScheduleCommandHandler.handle(
        CreateTaskScheduleCommand(
            identifier = TaskScheduleId(),
            taskIdentifier = taskIdentifier,
            start = taskScheduleDto.start,
            end = taskScheduleDto.end,
            slots = taskScheduleDto.slots))

    val schedule =
        taskScheduleQueryService.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskIdentifier)
    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(SCHEDULE_BY_TASK_ID_ENDPOINT)
            .buildAndExpand(taskIdentifier)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(schedule.version.toString())
        .body(taskScheduleResourceFactory.build(schedule))
  }

  @PostMapping(SCHEDULES_BATCH_ENDPOINT)
  open fun createTaskSchedules(
      @RequestBody
      @Valid
      createTaskScheduleBatchResources: UpdateBatchRequestResource<CreateTaskScheduleBatchResource>
  ): ResponseEntity<TaskScheduleListResource> {

    val createTaskScheduleCommands =
        assembleOfCreate(createTaskScheduleBatchResources.items).map {
          CreateTaskScheduleCommand(
              identifier = it.identifier ?: TaskScheduleId(),
              taskIdentifier = it.taskIdentifier,
              start = it.start,
              end = it.end,
              slots = it.slots)
        }

    createTaskScheduleBatchCommandHandler.handle(createTaskScheduleCommands)

    // The getIdentifiers() for the task schedule resources returns set of task ids
    // not schedule ids
    val schedules =
        taskScheduleQueryService.findByTaskIdentifiers(
            createTaskScheduleBatchResources.getIdentifiers().asTaskIds())
    return ResponseEntity.ok().body(taskScheduleListResourceFactory.build(schedules))
  }

  @GetMapping(SCHEDULE_BY_TASK_ID_ENDPOINT)
  open fun findByTaskIdentifier(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskScheduleResource> =
      taskScheduleQueryService.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskIdentifier).let {
        ResponseEntity.ok().eTag(it.version.toString()).body(taskScheduleResourceFactory.build(it))
      }

  @PostMapping(SCHEDULES_ENDPOINT)
  open fun findByBatchIdentifiers(
      @RequestBody @Valid batchRequestResource: BatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = TASK) identifierType: String
  ): ResponseEntity<TaskScheduleListResource> =
      when (identifierType) {
        TASK -> {
          val taskIdentifiers = batchRequestResource.ids.asTaskIds()
          val schedules = taskScheduleQueryService.findByTaskIdentifiers(taskIdentifiers)
          ResponseEntity.ok().body(taskScheduleListResourceFactory.build(schedules))
        }
        TASK_SCHEDULE -> {
          val taskScheduleIdentifiers = batchRequestResource.ids.asTaskScheduleIds()
          val schedules =
              taskScheduleQueryService.findByTaskScheduleIdentifiers(taskScheduleIdentifiers)
          ResponseEntity.ok().body(taskScheduleListResourceFactory.build(schedules))
        }
        else -> {
          throw BatchIdentifierTypeNotSupportedException(
              COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
        }
      }

  @GetMapping(SCHEDULE_BY_SCHEDULE_ID_ENDPOINT)
  open fun find(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(PATH_VARIABLE_SCHEDULE_ID) scheduleIdentifier: TaskScheduleId
  ): ResponseEntity<TaskScheduleResource> {
    val schedule = taskScheduleQueryService.find(scheduleIdentifier, projectIdentifier)

    return ResponseEntity.ok()
        .eTag(schedule.version.toString())
        .body(taskScheduleResourceFactory.build(schedule))
  }

  @PutMapping(SCHEDULE_BY_TASK_ID_ENDPOINT)
  open fun updateTaskSchedule(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @RequestBody @Valid resource: UpdateTaskScheduleResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<TaskScheduleResource> {
    val taskScheduleDto = assemble(resource)

    val taskScheduleId =
        taskScheduleService.findTaskScheduleIdentifierByTaskIdentifier(taskIdentifier)
    updateTaskScheduleCommandHandler.handle(
        UpdateTaskScheduleCommand(
            identifier = taskScheduleId,
            taskIdentifier = taskIdentifier,
            start = taskScheduleDto.start,
            version = eTag.toVersion(),
            end = taskScheduleDto.end,
            slots = taskScheduleDto.slots))
    return findByTaskIdentifier(taskIdentifier)
  }

  @PutMapping(SCHEDULES_BATCH_ENDPOINT)
  open fun updateTaskSchedules(
      @RequestBody
      @Valid
      updateTaskScheduleBatchResources: UpdateBatchRequestResource<UpdateTaskScheduleBatchResource>
  ): ResponseEntity<TaskScheduleListResource> {
    val mapSchedulesByTaskIdentifier =
        taskScheduleQueryService
            .findByTaskIdentifiers(updateTaskScheduleBatchResources.getIdentifiers().asTaskIds())
            .associate { it.taskIdentifier to it.identifier }

    val updateTaskScheduleCommands =
        assembleOfUpdate(updateTaskScheduleBatchResources.items).map {
          UpdateTaskScheduleCommand(
              identifier = requireNotNull(mapSchedulesByTaskIdentifier[it.taskIdentifier]),
              taskIdentifier = it.taskIdentifier,
              version = it.version,
              start = it.start,
              end = it.end,
              slots = it.slots)
        }

    updateTaskScheduleBatchCommandHandler.handle(updateTaskScheduleCommands)

    // The getIdentifiers() for the task schedule resources returns set of task ids
    // not schedule ids
    val schedules =
        taskScheduleQueryService.findByTaskIdentifiers(
            updateTaskScheduleBatchResources.getIdentifiers().asTaskIds())
    return ResponseEntity.ok().body(taskScheduleListResourceFactory.build(schedules))
  }

  @DeleteMapping(SCHEDULE_BY_TASK_ID_ENDPOINT)
  open fun deleteByTaskIdentifier(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<Void> {
    deleteTaskScheduleCommandHandler.handle(
        DeleteTaskScheduleCommand(taskIdentifier = taskIdentifier, eTag = eTag))
    return ResponseEntity.noContent().build()
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_TASK_ID = "taskId"
    const val PATH_VARIABLE_SCHEDULE_ID = "scheduleId"

    const val SCHEDULES_ENDPOINT = "/projects/tasks/schedules"
    const val SCHEDULE_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/schedule"

    const val SCHEDULE_BY_SCHEDULE_ID_ENDPOINT =
        "/projects/{projectId}/tasks/schedules/{scheduleId}"

    const val SCHEDULES_BATCH_ENDPOINT = "/projects/tasks/schedules/batch"
  }
}
