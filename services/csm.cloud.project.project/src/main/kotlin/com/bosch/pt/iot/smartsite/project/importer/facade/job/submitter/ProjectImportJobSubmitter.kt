/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.job.submitter

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_ALREADY_RUNNING
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_EXISTING_DATA
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportService
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobContext
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobType.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.FAILED
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.IN_PROGRESS
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import datadog.trace.api.Trace
import java.util.UUID
import java.util.UUID.randomUUID
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ProjectImportJobSubmitter(
    private val blobStorageRepository: ImportBlobStorageRepository,
    private val jobIntegrationService: JobIntegrationService,
    private val projectQueryService: ProjectQueryService,
    private val projectImportService: ProjectImportService
) {

  @Suppress("ThrowsCount")
  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#projectIdentifier)")
  open fun enqueueImportJob(projectIdentifier: ProjectId, eTag: ETag): UUID {

    val project = requireNotNull(projectQueryService.findOneByIdentifier(projectIdentifier))
    var projectImport = requireNotNull(projectImportService.findImportObject(projectIdentifier))
    val blob = requireNotNull(blobStorageRepository.find(projectImport.blobName))

    // Check optimistic locking
    eTag.verify(requireNotNull(projectImport.version))

    // Check constraints
    if (!projectImportService.isImportPossible(project)) {
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_EXISTING_DATA)
    }

    if (projectImport.status == IN_PROGRESS) {
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_ALREADY_RUNNING)
    }

    // Update entity
    projectImport.status = IN_PROGRESS
    projectImport.jobId = randomUUID()
    projectImport = projectImportService.save(projectImport)

    // Send event to import asynchronously
    try {
      return jobIntegrationService.enqueueJob(
          PROJECT_IMPORT.name,
          SecurityContextHelper.getInstance().getCurrentUser().identifier!!,
          ProjectImportJobContext(ResourceReference.from(project), blob.metadata.get("filename")!!),
          ProjectImportCommand(LocaleContextHolder.getLocale(), project.identifier),
          requireNotNull(projectImport.jobId))
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      projectImport.status = FAILED
      projectImportService.save(projectImport)
      throw e
    }
  }
}
