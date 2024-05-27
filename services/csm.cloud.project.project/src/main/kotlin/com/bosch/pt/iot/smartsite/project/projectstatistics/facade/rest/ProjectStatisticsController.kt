/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectstatistics.boundary.ProjectStatisticsService
import com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest.resource.response.ProjectStatisticsResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class ProjectStatisticsController(
    private val projectStatisticsService: ProjectStatisticsService
) {

  @GetMapping(STATISTICS_BY_PROJECT_ID_ENDPOINT)
  open fun getProjectStatistics(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId
  ): ResponseEntity<ProjectStatisticsResource> =
      ResponseEntity.ok(
          ProjectStatisticsResource(
              projectStatisticsService.getTaskStatistics(projectIdentifier),
              projectStatisticsService.getTopicStatistics(projectIdentifier)))

  companion object {
    const val STATISTICS_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/statistics"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
  }
}
