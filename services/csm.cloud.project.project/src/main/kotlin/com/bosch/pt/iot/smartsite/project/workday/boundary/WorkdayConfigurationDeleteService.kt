/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workday.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class WorkdayConfigurationDeleteService(
    private val workdayConfigurationRepository: WorkdayConfigurationRepository
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun deleteByProjectId(projectId: Long) {
    // Due to @MapsId, we can delete by projectId because projectId == workdayConfigurationId
    if (workdayConfigurationRepository.existsById(projectId)) {
      workdayConfigurationRepository.deleteById(projectId)
    }
  }
}
