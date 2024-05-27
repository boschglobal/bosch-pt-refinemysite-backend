/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import java.util.UUID

data class RescheduleResultResource(
    val successful: SuccessfulResource,
    val failed: FailedResource
) {

  data class SuccessfulResource(
      val milestones: Collection<MilestoneId>,
      val tasks: Collection<UUID>
  )

  data class FailedResource(val milestones: Collection<MilestoneId>, val tasks: Collection<UUID>)
}
