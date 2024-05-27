/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.repository.WorkDayConfigurationRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class WorkDayConfigurationQueryService(
    private val workDayConfigurationRepository: WorkDayConfigurationRepository
) {

  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<WorkDayConfiguration> =
      workDayConfigurationRepository.findAllByProjectInAndDeletedFalse(projectIds)

  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<WorkDayConfiguration> =
      workDayConfigurationRepository.findAllByProjectIn(projectIds)
}
