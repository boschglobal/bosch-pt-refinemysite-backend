/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class MilestoneListSnapshotEntityCache(private val repository: MilestoneListRepository) :
    AbstractSnapshotEntityCache<MilestoneListId, MilestoneList>() {

  override fun loadOneFromDatabase(identifier: MilestoneListId) =
      repository.findOneByIdentifier(identifier)
}
