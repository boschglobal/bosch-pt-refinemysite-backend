/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.JobResponseResource
import com.bosch.pt.iot.smartsite.project.exporter.api.MilestoneExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.submitter.ProjectExportJobSubmitter
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.ProjectExportFormatRequestEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.ProjectExportFormatRequestEnum.PRIMAVERA_P6_XML
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.ProjectExportFormatRequestEnum.ZIP_ARCHIVE
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class ProjectExportController(
    private val projectExportJobSubmitter: ProjectExportJobSubmitter
) {

  @PostMapping(EXPORT_BY_PROJECT_ID_ENDPOINT)
  open fun export(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: UUID,
      @RequestBody projectExportRequestParameters: ProjectExportRequestParameters
  ): ResponseEntity<JobResponseResource> {
    return when (projectExportRequestParameters.format) {
      MS_PROJECT_XML,
      PRIMAVERA_P6_XML ->
          ResponseEntity.accepted()
              .body(
                  JobResponseResource(
                      projectExportJobSubmitter
                          .enqueueExportJob(
                              projectIdentifier,
                              projectExportRequestParameters.toProjectExportParameters())
                          .toString()))
      ZIP_ARCHIVE -> {
        ResponseEntity.accepted()
            .body(
                JobResponseResource(
                    projectExportJobSubmitter.enqueueExportZipJob(projectIdentifier).toString()))
      }
    }
  }

  data class ProjectExportRequestParameters(
      val format: ProjectExportFormatRequestEnum,
      val includeMilestones: Boolean? = true,
      val includeComments: Boolean?,
      val taskExportSchedulingType: TaskExportSchedulingType? =
          TaskExportSchedulingType.AUTO_SCHEDULED,
      val milestoneExportSchedulingType: MilestoneExportSchedulingType? =
          MilestoneExportSchedulingType.MANUALLY_SCHEDULED,
  ) {
    fun toProjectExportParameters(): ProjectExportParameters {
      require(format == MS_PROJECT_XML || format == PRIMAVERA_P6_XML)
      return ProjectExportParameters(
          ProjectExportFormatEnum.valueOf(format.name),
          requireNotNull(includeMilestones),
          requireNotNull(includeComments),
          requireNotNull(taskExportSchedulingType),
          requireNotNull(milestoneExportSchedulingType))
    }
  }

  enum class ProjectExportFormatRequestEnum {
    MS_PROJECT_XML,
    PRIMAVERA_P6_XML,
    ZIP_ARCHIVE
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val EXPORT_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/export"
  }
}
