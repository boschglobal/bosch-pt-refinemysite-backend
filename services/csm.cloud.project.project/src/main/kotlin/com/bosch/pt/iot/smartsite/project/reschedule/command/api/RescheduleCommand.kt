/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.api

import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto

data class RescheduleCommand(
    val shiftDays: Long,
    val useTaskCriteria: Boolean,
    val useMilestoneCriteria: Boolean,
    val taskCriteria: SearchTasksDto,
    val milestoneCriteria: SearchMilestonesDto,
    val projectIdentifier: ProjectId,
)
