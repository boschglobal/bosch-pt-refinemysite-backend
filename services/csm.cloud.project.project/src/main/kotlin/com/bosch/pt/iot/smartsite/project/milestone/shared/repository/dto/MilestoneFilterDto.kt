/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository.dto

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate

data class MilestoneFilterDto(
    @Deprecated("Remove once the controller support the project id in the URL")
    val projectIdentifier: ProjectId? = null,
    val types: Set<MilestoneTypeEnum> = emptySet(),
    val craftIdentifiers: Set<ProjectCraftId> = emptySet(),
    val workAreaIdentifiers: Set<WorkAreaIdOrEmpty> = emptySet(),
    val rangeStartDate: LocalDate? = null,
    val rangeEndDate: LocalDate? = null,
    val milestoneListIdentifiers: Set<MilestoneListId> = emptySet(),
    val header: Boolean? = null
)
