/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import jakarta.validation.Valid

data class RescheduleResource(
    val shiftDays: Long,
    val useTaskCriteria: Boolean,
    val useMilestoneCriteria: Boolean,
    @Valid val criteria: CriteriaResource
) {
  data class CriteriaResource(
      @Valid val milestones: FilterMilestoneListResource,
      @Valid val tasks: FilterTaskListResource
  )
}
