/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.dto

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.dto.MilestoneFilterDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate

data class SearchMilestonesDto(
    @Deprecated("Remove once the controller support the project id in the URL")
    val projectIdentifier: ProjectId? = null,
    val typesFilter: TypesFilterDto = TypesFilterDto(),
    val workAreas: WorkAreaFilterDto = WorkAreaFilterDto(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val milestoneListIdentifiers: Set<MilestoneListId> = emptySet(),
) {

  fun toMilestoneFilterDto() =
      MilestoneFilterDto(
          projectIdentifier,
          typesFilter.types,
          typesFilter.craftIdentifiers,
          workAreas.workAreaIdentifiers,
          from,
          to,
          milestoneListIdentifiers,
          workAreas.header)
}

data class WorkAreaFilterDto(
    val header: Boolean? = null,
    val workAreaIdentifiers: Set<WorkAreaIdOrEmpty> = emptySet(),
)

data class TypesFilterDto(
    val types: Set<MilestoneTypeEnum> = emptySet(),
    val craftIdentifiers: Set<ProjectCraftId> = emptySet()
)
