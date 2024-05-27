/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.query

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class WorkAreaListQueryService(
    private val workAreaListRepository: WorkAreaListRepository,
) {

  @Trace
  @NoPreAuthorize(usedByController = true)
  @Transactional(readOnly = true)
  open fun findOneWithDetailsByIdentifier(workAreaListIdentifier: WorkAreaListId): WorkAreaList? =
      workAreaListRepository.findOneWithDetailsByIdentifier(workAreaListIdentifier)

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findOneWithDetailsByProjectIdentifier(projectIdentifier: ProjectId): WorkAreaList =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)
          ?: throw AggregateNotFoundException(
              WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND, projectIdentifier.toString())
}
