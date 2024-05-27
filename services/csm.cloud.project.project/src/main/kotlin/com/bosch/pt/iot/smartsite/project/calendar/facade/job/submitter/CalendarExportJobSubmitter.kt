/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.job.submitter

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsCsvCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsJsonCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsPdfCommand
import com.bosch.pt.iot.smartsite.project.calendar.boundary.ExportTimeRangeValidator
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_CSV
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_JSON
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_PDF
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.ExportCalendarJobContext
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import java.util.UUID
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class CalendarExportJobSubmitter(
    private val jobIntegrationService: JobIntegrationService,
    private val projectQueryService: ProjectQueryService
) {

  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun enqueueCsvExportJob(
      projectIdentifier: ProjectId,
      exportParameters: CalendarExportParameters
  ): UUID {
    ExportTimeRangeValidator.validate(exportParameters.from, exportParameters.to)
    val project = projectQueryService.findOneByIdentifier(projectIdentifier)!!

    return jobIntegrationService.enqueueJob(
        CALENDAR_EXPORT_CSV.name,
        SecurityContextHelper.getInstance().getCurrentUser().identifier!!,
        ExportCalendarJobContext(ResourceReference.from(project)),
        ExportCalendarAsCsvCommand(
            locale = LocaleContextHolder.getLocale(),
            projectIdentifier = projectIdentifier,
            calendarExportParameters = exportParameters))
  }

  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun enqueueJsonExportJob(
      projectIdentifier: ProjectId,
      exportParameters: CalendarExportParameters
  ): UUID {
    ExportTimeRangeValidator.validate(exportParameters.from, exportParameters.to)
    val project = projectQueryService.findOneByIdentifier(projectIdentifier)!!

    return jobIntegrationService.enqueueJob(
        CALENDAR_EXPORT_JSON.name,
        SecurityContextHelper.getInstance().getCurrentUser().identifier!!,
        ExportCalendarJobContext(ResourceReference.from(project)),
        ExportCalendarAsJsonCommand(
            locale = LocaleContextHolder.getLocale(),
            projectIdentifier = projectIdentifier,
            calendarExportParameters = exportParameters))
  }

  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun enqueuePdfExportJob(
      projectIdentifier: ProjectId,
      exportParameters: CalendarExportParameters,
      token: String
  ): UUID {
    ExportTimeRangeValidator.validate(exportParameters.from, exportParameters.to)
    val project = projectQueryService.findOneByIdentifier(projectIdentifier)!!

    return jobIntegrationService.enqueueJob(
        CALENDAR_EXPORT_PDF.name,
        SecurityContextHelper.getInstance().getCurrentUser().identifier!!,
        ExportCalendarJobContext(ResourceReference.from(project)),
        ExportCalendarAsPdfCommand(
            locale = LocaleContextHolder.getLocale(),
            projectIdentifier = projectIdentifier,
            calendarExportParameters = exportParameters,
            token = token))
  }
}
