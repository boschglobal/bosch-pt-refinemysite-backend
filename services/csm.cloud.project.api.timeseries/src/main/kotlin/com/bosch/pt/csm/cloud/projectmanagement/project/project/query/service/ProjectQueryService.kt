/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.repository.ProjectRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class ProjectQueryService(private val projectRepository: ProjectRepository) {

  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifierIn(projectIds: List<ProjectId>): List<Project> =
      projectRepository.findAllByIdentifierIn(projectIds)
}
