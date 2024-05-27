/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import com.bosch.pt.iot.smartsite.project.taskcopy.command.handler.TaskCopyBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest.resource.request.CopyTaskResource
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class TaskCopyController(
    private val taskCopyBatchCommandHandler: TaskCopyBatchCommandHandler,
    private val taskQueryService: TaskQueryService,
    private val taskBatchResourceFactory: TaskBatchResourceFactory
) {

  @PostMapping(COPY_TASKS_ENDPOINT)
  open fun copy(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid resource: CreateBatchRequestResource<CopyTaskResource>
  ): ResponseEntity<BatchResponseResource<TaskResource>> {

    val taskCopiesIdentifiers =
        taskCopyBatchCommandHandler.handle(resource.items.map { it.toCommand() }, projectIdentifier)

    return taskQueryService.findTasksWithDetails(taskCopiesIdentifiers).let {
      ResponseEntity.ok().body(taskBatchResourceFactory.build(it, projectIdentifier))
    }
  }
  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val COPY_TASKS_ENDPOINT = "/projects/{projectId}/tasks/copy"
  }
}
