/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class MilestoneSnapshotEntityCache(private val repository: MilestoneRepository) :
    AbstractSnapshotEntityCache<MilestoneId, Milestone>() {

  override fun loadOneFromDatabase(identifier: MilestoneId) =
      repository.findOneByIdentifier(identifier)

  open fun loadAllFromDatabase(identifiers: List<MilestoneId>) =
      repository.findAllByIdentifierIn(identifiers)
}
