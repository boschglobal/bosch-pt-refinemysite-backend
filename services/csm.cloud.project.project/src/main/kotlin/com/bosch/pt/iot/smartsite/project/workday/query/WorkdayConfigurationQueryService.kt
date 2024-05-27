/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.query

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class WorkdayConfigurationQueryService(
    private val workdayConfigurationRepository: WorkdayConfigurationRepository
) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun findOneByProjectIdentifier(projectIdentifier: ProjectId): WorkdayConfiguration =
      requireNotNull(
          workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier))
}
