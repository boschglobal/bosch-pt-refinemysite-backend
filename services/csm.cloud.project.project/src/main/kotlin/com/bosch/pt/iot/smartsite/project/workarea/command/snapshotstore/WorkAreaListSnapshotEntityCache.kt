/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class WorkAreaListSnapshotEntityCache(private val repository: WorkAreaListRepository) :
    AbstractSnapshotEntityCache<WorkAreaListId, WorkAreaList>() {

  override fun loadOneFromDatabase(identifier: WorkAreaListId) =
      repository.findOneByIdentifier(identifier)
}
