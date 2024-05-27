/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectImportDeleteService(
    private val projectImportRepository: ProjectImportRepository,
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun deleteByJobId(jobId: UUID) = projectImportRepository.deleteByJobId(jobId)

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun deleteByProjectIdentifier(projectIdentifier: ProjectId) =
      projectImportRepository.deleteByProjectIdentifier(projectIdentifier)
}
