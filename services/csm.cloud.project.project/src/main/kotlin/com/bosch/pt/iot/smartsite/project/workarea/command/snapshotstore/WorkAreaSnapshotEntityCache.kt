/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class WorkAreaSnapshotEntityCache(private val repository: WorkAreaRepository) :
    AbstractSnapshotEntityCache<WorkAreaId, WorkArea>() {

  override fun loadOneFromDatabase(identifier: WorkAreaId) =
      repository.findOneByIdentifier(identifier)
}
