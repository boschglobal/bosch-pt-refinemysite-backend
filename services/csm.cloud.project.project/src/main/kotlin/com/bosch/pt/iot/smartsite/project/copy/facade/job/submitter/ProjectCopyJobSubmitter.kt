/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.facade.job.submitter

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.copy.api.ProjectCopyCommand
import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyParameters
import com.bosch.pt.iot.smartsite.project.copy.facade.job.dto.ProjectCopyJobContext
import com.bosch.pt.iot.smartsite.project.copy.facade.job.dto.ProjectCopyJobType
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ProjectCopyJobSubmitter(
    private val jobIntegrationService: JobIntegrationService,
    private val projectQueryService: ProjectQueryService
) {

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun enqueueCopyJob(
      projectIdentifier: UUID,
      projectCopyParameters: ProjectCopyParameters
  ): UUID {

    val project =
        requireNotNull(projectQueryService.findOneByIdentifier(projectIdentifier.asProjectId()))

    return jobIntegrationService.enqueueJob(
        ProjectCopyJobType.PROJECT_COPY.name,
        checkNotNull(SecurityContextHelper.getInstance().getCurrentUser().identifier),
        ProjectCopyJobContext(ResourceReference.from(project)),
        ProjectCopyCommand(
            LocaleContextHolder.getLocale(),
            checkNotNull(project.identifier.toUuid()),
            projectCopyParameters))
  }
}
