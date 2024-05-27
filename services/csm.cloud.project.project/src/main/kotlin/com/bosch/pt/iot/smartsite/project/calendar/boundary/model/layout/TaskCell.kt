/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout

import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import java.time.LocalDate

data class TaskCell(
    val name: String,
    val company: String,
    val craftName: String,
    val craftColor: String,
    val status: TaskStatusEnum,
    val start: LocalDate,
    val end: LocalDate,
    val hasTaskConstraint: Boolean,
    val dayCards: List<DayCardCell>
) {

  fun getStatusIcon() =
      when (status) {
        DRAFT -> "#task-status-draft"
        OPEN -> "#task-status-open"
        STARTED -> "#task-status-progress"
        CLOSED -> "#task-status-done"
        ACCEPTED -> "#task-status-accepted"
      }
}
