/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskstatistics.boundary.TaskStatisticsService
import com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest.resource.response.TaskStatisticsResource
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@Validated
@RestController
open class TaskStatisticsController(private val taskStatisticsService: TaskStatisticsService) {

  @GetMapping(STATISTICS_BY_TASK_ID_ENDPOINT)
  open fun findTaskStatistics(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId
  ): ResponseEntity<TaskStatisticsResource> =
      ResponseEntity.ok(
          TaskStatisticsResource(taskStatisticsService.findTaskStatistics(taskIdentifier)))

  companion object {
    const val STATISTICS_BY_TASK_ID_ENDPOINT = "/tasks/{taskId}/statistics"
    const val PATH_VARIABLE_TASK_ID = "taskId"
  }
}
