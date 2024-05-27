/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskListResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_SEARCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class TaskSearchController(
    private val taskListResourceFactory: TaskListResourceFactory,
    private val taskQueryService: TaskQueryService
) {

  @PostMapping(TASKS_SEARCH_ENDPOINT)
  open fun search(
      @PathVariable("projectId") projectId: ProjectId,
      @RequestBody filter: FilterTaskListResource,
      @PageableDefault(sort = ["name", "status"], direction = ASC, size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<TaskListResource> {
    val tasks =
        taskQueryService.findTasksWithDetailsForFilters(
            filter.toSearchTasksDto(projectId), pageable)

    return ResponseEntity(taskListResourceFactory.build(tasks, pageable, projectId), OK)
  }
}
