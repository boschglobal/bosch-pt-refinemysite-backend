/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.TypesFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.WorkAreaFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate

data class FilterMilestoneListResource(
    val types: TypesFilter = TypesFilter(),
    val workAreas: WorkAreaFilter = WorkAreaFilter(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val milestoneListIds: Set<MilestoneListId> = emptySet(),
) {
  fun toSearchMilestonesDto(projectRef: ProjectId) =
      SearchMilestonesDto(
          projectIdentifier = projectRef,
          typesFilter = types.toTypeFilterDto(),
          workAreas = workAreas.toWorkAreaFilterDto(),
          from = from,
          to = to,
          milestoneListIdentifiers = milestoneListIds)

  data class WorkAreaFilter(
      val header: Boolean? = null,
      val workAreaIds: Set<WorkAreaIdOrEmpty> = emptySet()
  ) {
    fun toWorkAreaFilterDto() =
        WorkAreaFilterDto(header = header, workAreaIdentifiers = workAreaIds)
  }

  data class TypesFilter(
      val types: Set<MilestoneTypeEnum> = emptySet(),
      val projectCraftIds: Set<ProjectCraftId> = emptySet()
  ) {
    fun toTypeFilterDto() = TypesFilterDto(types = types, craftIdentifiers = projectCraftIds)
  }
}
