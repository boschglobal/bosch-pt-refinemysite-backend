/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.repository.WorkAreaListRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class WorkAreaListQueryService(private val workAreaListRepository: WorkAreaListRepository) {

  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): Map<ProjectId, WorkAreaList> =
      workAreaListRepository.findAllByProjectInAndDeletedFalse(projectIds).associateBy {
        it.project
      }
}
