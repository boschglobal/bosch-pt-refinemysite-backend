/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.dto

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId

data class RescheduleResultDto(val successful: SuccessfulDto, val failed: FailedDto) {

  data class SuccessfulDto(val milestones: Collection<MilestoneId>, val tasks: Collection<TaskId>)

  data class FailedDto(val milestones: Collection<MilestoneId>, val tasks: Collection<TaskId>)
}
