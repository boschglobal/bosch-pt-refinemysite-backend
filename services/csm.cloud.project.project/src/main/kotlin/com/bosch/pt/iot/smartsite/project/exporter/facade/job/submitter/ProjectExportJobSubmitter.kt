/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.facade.job.submitter

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.i18n.Key.EXPORT_IMPOSSIBLE_FEATURE_DEACTIVATED
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportCommand
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportZipCommand
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ZipProjectExportService
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobContext
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobType.PROJECT_EXPORT
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobType.PROJECT_EXPORT_ZIP
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ProjectExportJobSubmitter(
    private val jobIntegrationService: JobIntegrationService,
    private val projectExportService: ProjectExportService,
    private val zipProjectExportService: ZipProjectExportService,
    private val projectQueryService: ProjectQueryService
) {

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun enqueueExportJob(
      projectIdentifier: UUID,
      projectExportParameters: ProjectExportParameters
  ): UUID {
    val project =
        requireNotNull(projectQueryService.findOneByIdentifier(projectIdentifier.asProjectId()))

    // Check constraints
    if (!projectExportService.isExportPossible(project)) {
      throw PreconditionViolationException(EXPORT_IMPOSSIBLE_FEATURE_DEACTIVATED)
    }

    return jobIntegrationService.enqueueJob(
        jobType = PROJECT_EXPORT.name,
        userIdentifier =
            checkNotNull(SecurityContextHelper.getInstance().getCurrentUser().identifier),
        context = ProjectExportJobContext(ResourceReference.from(project)),
        command =
            ProjectExportCommand(
                LocaleContextHolder.getLocale(),
                checkNotNull(project.identifier.toUuid()),
                projectExportParameters))
  }

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun enqueueExportZipJob(projectIdentifier: UUID): UUID {
    val project =
        requireNotNull(projectQueryService.findOneByIdentifier(projectIdentifier.asProjectId()))

    if (!zipProjectExportService.isExportPossible(project)) {
      throw PreconditionViolationException(EXPORT_IMPOSSIBLE_FEATURE_DEACTIVATED)
    }

    return jobIntegrationService.enqueueJob(
        jobType = PROJECT_EXPORT_ZIP.name,
        userIdentifier =
            checkNotNull(SecurityContextHelper.getInstance().getCurrentUser().identifier),
        context = ProjectExportJobContext(ResourceReference.from(project)),
        command =
            ProjectExportZipCommand(
                LocaleContextHolder.getLocale(), checkNotNull(project.identifier.toUuid())))
  }
}
