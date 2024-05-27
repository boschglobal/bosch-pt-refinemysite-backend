/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.service

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class MilestoneDeleteService(
    private val milestoneRepository: MilestoneRepository,
    private val milestoneListRepository: MilestoneListRepository
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun deleteByProjectId(projectId: Long) {
    val milestones = milestoneRepository.findAllByProjectId(projectId)
    if (milestones.isNotEmpty()) {
      milestoneRepository.deleteAllInBatch(milestones)
    }
    val milestoneLists = milestoneListRepository.findAllByProjectId(projectId)
    if (milestoneLists.isNotEmpty()) {
      milestoneListRepository.deleteAllInBatch(milestoneLists)
    }
  }
}
