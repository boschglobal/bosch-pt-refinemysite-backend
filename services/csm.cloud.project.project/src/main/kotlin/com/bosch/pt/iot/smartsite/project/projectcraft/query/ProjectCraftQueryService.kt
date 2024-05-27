/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.query

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectCraftQueryService(private val projectCraftRepository: ProjectCraftRepository) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@projectCraftAuthorizationComponent.hasReadPermissionOnProjectCraft(#identifier)")
  open fun findOneByIdentifier(identifier: ProjectCraftId): ProjectCraft =
      projectCraftRepository.findOneWithDetailsByIdentifier(identifier)!!

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  open fun countByProjectIdentifier(projectIdentifier: ProjectId): Long =
      projectCraftRepository.countByProjectIdentifier(projectIdentifier)
}
