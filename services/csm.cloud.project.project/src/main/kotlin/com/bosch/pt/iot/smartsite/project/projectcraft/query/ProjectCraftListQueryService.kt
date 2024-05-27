/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.query

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectCraftListQueryService(
    private val projectCraftListRepository: ProjectCraftListRepository
) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun findOneByProject(projectIdentifier: ProjectId): ProjectCraftList =
      projectCraftListRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)
          ?: error("Could not find project craft list for the project $projectIdentifier")
}
