/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.service

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaListRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service

@Service
class WorkAreaListService(private val workAreaListRepository: WorkAreaListRepository) {
  @Trace fun save(workAreaList: WorkAreaList) = workAreaListRepository.save(workAreaList)
}
