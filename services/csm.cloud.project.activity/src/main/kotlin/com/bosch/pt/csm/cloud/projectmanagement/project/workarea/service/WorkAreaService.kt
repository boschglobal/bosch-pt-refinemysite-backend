/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.service

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class WorkAreaService(private val workAreaRepository: WorkAreaRepository) {

  @Trace fun save(workArea: WorkArea) = workAreaRepository.save(workArea)

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      workAreaRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      workAreaRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      workAreaRepository.deleteByVersion(identifier, version, projectIdentifier)
}
