/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request

import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.boundary.dto.QuickFilterDto
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class SaveQuickFilterResource(
    @field:Size(min = 1, max = MAX_NAME_LENGTH) val name: String,
    val highlight: Boolean,
    val useMilestoneCriteria: Boolean,
    val useTaskCriteria: Boolean,
    val criteria: CriteriaResource,
) {

  fun toQuickFilterDto(projectRef: ProjectId) =
      QuickFilterDto(
          name = name,
          highlight = highlight,
          useMilestoneCriteria = useMilestoneCriteria,
          useTaskCriteria = useTaskCriteria,
          taskCriteria = criteria.tasks.toSearchTasksDto(projectRef),
          milestoneCriteria = criteria.milestones.toSearchMilestonesDto(projectRef),
          projectRef = projectRef)

  data class CriteriaResource(
      @Valid val milestones: FilterMilestoneListResource,
      @Valid val tasks: FilterTaskListResource
  )
}
