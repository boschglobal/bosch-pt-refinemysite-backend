/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.JobResponseResource
import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyParameters
import com.bosch.pt.iot.smartsite.project.copy.facade.job.submitter.ProjectCopyJobSubmitter
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class ProjectCopyController(private val projectCopyJobSubmitter: ProjectCopyJobSubmitter) {

  @PostMapping(COPY_BY_PROJECT_ID_ENDPOINT)
  open fun copy(
      @PathVariable(ProjectExportController.PATH_VARIABLE_PROJECT_ID) projectIdentifier: UUID,
      @RequestBody projectCopyParameters: ProjectCopyParameters
  ): ResponseEntity<JobResponseResource> {
    return ResponseEntity.accepted()
        .body(
            JobResponseResource(
                projectCopyJobSubmitter
                    .enqueueCopyJob(projectIdentifier, projectCopyParameters)
                    .toString()))
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val COPY_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/copy"
  }
}
