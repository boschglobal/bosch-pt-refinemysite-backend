/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout

data class RowCell(
    val name: String,
    val tasks: Collection<TaskCell>,
    val milestones: Collection<MilestoneCell>,
    val position: Int,
    val type: RowCellType
)

enum class RowCellType {
  GLOBAL_MILESTONE,
  WORK_AREA,
  WITH_NO_WORK_AREA;

  val isGlobalMilestone: Boolean
    get() = this == GLOBAL_MILESTONE
  val isWorkArea: Boolean
    get() = this == WORK_AREA
  val isWithNoWorkArea: Boolean
    get() = this == WITH_NO_WORK_AREA
}
