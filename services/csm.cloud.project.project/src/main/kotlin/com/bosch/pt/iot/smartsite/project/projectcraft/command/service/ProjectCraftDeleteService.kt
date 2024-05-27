/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectcraft.command.service

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectCraftDeleteService(
    private val projectCraftRepository: ProjectCraftRepository,
    private val projectCraftListRepository: ProjectCraftListRepository
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun deleteByProjectId(projectId: Long) {
    val projectCrafts = projectCraftRepository.findAllByProjectId(projectId)
    if (projectCrafts.isNotEmpty()) {
      projectCraftRepository.deleteAllInBatch(projectCrafts)
    }
    val projectCraftLists = projectCraftListRepository.findAllByProjectId(projectId)
    if (projectCraftLists.isNotEmpty())
        projectCraftListRepository.deleteAllInBatch(projectCraftLists)
  }
}
