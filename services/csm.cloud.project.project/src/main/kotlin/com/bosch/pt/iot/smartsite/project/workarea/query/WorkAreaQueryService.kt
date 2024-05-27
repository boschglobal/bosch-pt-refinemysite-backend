/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.query

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class WorkAreaQueryService(private val workAreaRepository: WorkAreaRepository) {

  @Trace
  @PreAuthorize("@workAreaAuthorizationComponent.hasReadPermissionOnWorkArea(#workAreaIdentifier)")
  @Transactional(readOnly = true)
  open fun findOneWithDetailsByIdentifier(workAreaIdentifier: WorkAreaId): WorkArea? =
      workAreaRepository.findOneWithDetailsByIdentifier(workAreaIdentifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun countByProjectIdentifier(projectIdentifier: ProjectId): Long =
      workAreaRepository.countByProjectIdentifier(projectIdentifier)
}
