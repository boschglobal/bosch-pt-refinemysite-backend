/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.api

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate
import kotlin.Int.Companion.MAX_VALUE

data class CreateMilestoneCommand(
    val identifier: MilestoneId,
    val projectRef: ProjectId,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val description: String? = null,
    val craftRef: ProjectCraftId? = null,
    // if position is not specified, append at the end of the milestone list
    val workAreaRef: WorkAreaId? = null,
    val position: Int = MAX_VALUE
)

data class UpdateMilestoneCommand(
    val identifier: MilestoneId,
    val version: Long,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val description: String? = null,
    val craftRef: ProjectCraftId? = null,
    val workAreaRef: WorkAreaId? = null,
    val position: Int? = null
)

data class DeleteMilestoneCommand(val identifier: MilestoneId, val version: Long)
