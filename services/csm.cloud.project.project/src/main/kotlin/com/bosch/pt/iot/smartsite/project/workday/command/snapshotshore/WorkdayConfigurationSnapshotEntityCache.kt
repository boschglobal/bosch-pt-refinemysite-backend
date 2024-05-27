/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class WorkdayConfigurationSnapshotEntityCache(
    private val repository: WorkdayConfigurationRepository
) : AbstractSnapshotEntityCache<WorkdayConfigurationId, WorkdayConfiguration>() {

  override fun loadOneFromDatabase(identifier: WorkdayConfigurationId) =
      repository.findOneWithDetailsByIdentifier(identifier)
}
