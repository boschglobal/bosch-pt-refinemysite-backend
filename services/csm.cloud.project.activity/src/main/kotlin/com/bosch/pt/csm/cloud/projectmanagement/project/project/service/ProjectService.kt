/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.repository.ProjectRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class ProjectService(private val projectRepository: ProjectRepository) {

  @Trace fun save(project: Project) = projectRepository.save(project)

  @Trace fun delete(identifier: UUID) = projectRepository.deleteByProjectIdentifier(identifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long) =
      projectRepository.deleteByIdentifierIdentifierAndIdentifierVersion(identifier, version)
}
