/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.repository.WorkAreaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class WorkAreaQueryService(private val workAreaRepository: WorkAreaRepository) {

  @Cacheable(cacheNames = ["work-areas-by-identifiers-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiersAndDeletedFalse(workAreaIds: List<WorkAreaId>): List<WorkArea> =
      workAreaRepository.findAllByIdentifierInAndDeletedFalse(workAreaIds)

  @Cacheable(cacheNames = ["work-areas-by-projects-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<WorkArea> =
      workAreaRepository.findAllByProjectInAndDeletedFalse(projectIds)

  @Cacheable(cacheNames = ["work-areas-by-projects"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<WorkArea> =
      workAreaRepository.findAllByProjectIn(projectIds)
}
