/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class WorkAreaService(private val workAreaRepository: WorkAreaRepository) {

  @Trace fun save(workArea: WorkArea): WorkArea = workAreaRepository.save(workArea)

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID): WorkArea =
      workAreaRepository.findLatest(identifier, projectIdentifier)

  @Trace
  fun deleteWorkArea(workAreaIdentifier: UUID, projectIdentifier: UUID) =
      workAreaRepository.deleteWorkArea(workAreaIdentifier, projectIdentifier)
}
