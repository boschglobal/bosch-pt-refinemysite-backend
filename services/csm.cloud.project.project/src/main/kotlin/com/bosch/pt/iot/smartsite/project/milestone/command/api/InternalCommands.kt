/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.api

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate

/*
 * This file holds aggregate-internal commands. Do not use from outside the milestone aggregate!
 */

data class AddMilestoneToListCommand(
    val identifier: MilestoneListId,
    val milestoneRef: MilestoneId,
    val position: Int = 0
)

/** Creates a milestone list containing the given milestone. */
data class CreateMilestoneListCommand(
    val projectRef: ProjectId,
    val date: LocalDate,
    val header: Boolean,
    val workAreaRef: WorkAreaId? = null,
    val milestoneRef: MilestoneId,
)

data class ReorderMilestoneListCommand(
    val identifier: MilestoneListId,
    val milestoneRef: MilestoneId,
    val position: Int = 0
)

/**
 * Deletes the milestone list. The list can only be deleted when it contains exactly one milestone.
 * Otherwise, this command will fail.
 */
data class DeleteMilestoneListCommand(val identifier: MilestoneListId)

data class RemoveMilestoneFromListCommand(
    val identifier: MilestoneId,
    val milestoneListRef: MilestoneListId
)
