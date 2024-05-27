/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.command.service

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class WorkAreaDeleteService(
    private val workAreaRepository: WorkAreaRepository,
    private val workAreaListRepository: WorkAreaListRepository
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun deleteByProjectId(projectId: Long) {
    val workAreas = workAreaRepository.findAllByProjectId(projectId)
    if (workAreas.isNotEmpty()) {
      workAreaRepository.deleteAllInBatch(workAreas)
    }
    val workAreaLists = workAreaListRepository.findAllByProjectId(projectId)
    if (workAreaLists.isNotEmpty()) {
      workAreaListRepository.deleteAllInBatch(workAreaLists)
    }
  }
}
