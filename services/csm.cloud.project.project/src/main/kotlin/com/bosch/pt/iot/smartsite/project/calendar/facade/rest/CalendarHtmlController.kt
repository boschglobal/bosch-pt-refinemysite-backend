/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.boundary.CalendarExportHtmlService
import com.bosch.pt.iot.smartsite.project.calendar.boundary.ExportTimeRangeValidator
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import jakarta.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_XHTML_XML_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class CalendarHtmlController(
    private val calendarExportHtmlService: CalendarExportHtmlService
) {

  @PostMapping(
      value = [EXPORT_BY_PROJECT_ID_ENDPOINT],
      consumes = [APPLICATION_JSON_VALUE],
      produces = [APPLICATION_XHTML_XML_VALUE])
  open fun exportHtml(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid calendarExportResource: CalendarExportParameters
  ): ResponseEntity<String> {
    ExportTimeRangeValidator.validate(calendarExportResource.from, calendarExportResource.to)
    return calendarExportHtmlService.generateHtml(calendarExportResource, projectIdentifier).let {
      ResponseEntity.ok().body(it)
    }
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val EXPORT_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/calendar/export"
  }
}
