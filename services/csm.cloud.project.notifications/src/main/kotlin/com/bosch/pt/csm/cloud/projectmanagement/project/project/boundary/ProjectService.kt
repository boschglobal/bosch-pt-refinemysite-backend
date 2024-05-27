/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.repository.ProjectRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class ProjectService(private val projectRepository: ProjectRepository) {

  @Trace fun save(project: Project): Project = projectRepository.save(project)

  @Trace fun findLatest(identifier: UUID): Project = projectRepository.findLatest(identifier)

  @Trace
  fun findDisplayName(projectIdentifier: UUID): String =
      projectRepository.findDisplayName(projectIdentifier)

  @Trace
  fun deleteProjectAndAllRelatedDocuments(projectIdentifier: UUID) =
      projectRepository.deleteProjectAndAllRelatedDocuments(projectIdentifier)
}
