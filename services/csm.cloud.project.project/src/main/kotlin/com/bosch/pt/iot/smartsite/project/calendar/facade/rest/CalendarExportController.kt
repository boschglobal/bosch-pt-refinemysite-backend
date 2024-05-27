/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.JobResponseResource
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.submitter.CalendarExportJobSubmitter
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class CalendarExportController(
    private val calendarExportJobSubmitter: CalendarExportJobSubmitter
) {

  @PostMapping(EXPORT_PDF_BY_PROJECT_ID_ENDPOINT)
  open fun exportPdf(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid calendarExportResource: CalendarExportParameters,
      @RequestHeader(name = "Authorization") token: String
  ): ResponseEntity<JobResponseResource> {
    val jobIdentifier =
        calendarExportJobSubmitter.enqueuePdfExportJob(
            projectIdentifier, calendarExportResource, token)
    return ResponseEntity.accepted().body(JobResponseResource(jobIdentifier.toString()))
  }

  @PostMapping(EXPORT_JSON_BY_PROJECT_ID_ENDPOINT)
  open fun exportJson(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid calendarExportResource: CalendarExportParameters
  ): ResponseEntity<JobResponseResource> {
    val jobIdentifier =
        calendarExportJobSubmitter.enqueueJsonExportJob(projectIdentifier, calendarExportResource)
    return ResponseEntity.accepted().body(JobResponseResource(jobIdentifier.toString()))
  }

  @PostMapping(EXPORT_CSV_BY_PROJECT_ID_ENDPOINT)
  open fun exportCsv(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid calendarExportResource: CalendarExportParameters
  ): ResponseEntity<JobResponseResource> {
    val jobIdentifier =
        calendarExportJobSubmitter.enqueueCsvExportJob(projectIdentifier, calendarExportResource)
    return ResponseEntity.accepted().body(JobResponseResource(jobIdentifier.toString()))
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val EXPORT_CSV_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/calendar/export/csv"
    const val EXPORT_JSON_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/calendar/export/json"
    const val EXPORT_PDF_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/calendar/export/pdf"
  }
}
