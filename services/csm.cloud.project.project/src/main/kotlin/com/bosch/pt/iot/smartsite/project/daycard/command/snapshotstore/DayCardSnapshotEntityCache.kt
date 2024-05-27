/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class DayCardSnapshotEntityCache(private val repository: DayCardRepository) :
    AbstractSnapshotEntityCache<DayCardId, DayCard>() {

  override fun loadOneFromDatabase(identifier: DayCardId) =
      repository.findEntityByIdentifier(identifier)
}
