/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.repository.ProjectCraftRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class ProjectCraftQueryService(private val projectCraftRepository: ProjectCraftRepository) {

  @Cacheable(cacheNames = ["project-crafts-by-identifiers"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiers(craftIds: List<ProjectCraftId>): Map<ProjectCraftId, ProjectCraft> =
      projectCraftRepository.findAllByIdentifierIn(craftIds).associateBy { it.identifier }

  @Cacheable(cacheNames = ["project-crafts-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<ProjectCraft> =
      projectCraftRepository.findAllByProjectInAndDeletedFalse(projectIds)

  @Cacheable(cacheNames = ["project-crafts-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<ProjectCraft> =
      projectCraftRepository.findAllByProjectIn(projectIds)
}
