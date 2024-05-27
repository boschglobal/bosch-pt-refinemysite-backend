/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.BLOCKER
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.EMPTY
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.MERGE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.MILESTONE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.MILESTONE_HEADER
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.TASK
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.WORK_AREA_HEADER

data class CalendarCell(
    val content: Any,
    val width: Int,
    var height: Int,
    val color: String,
    val isRender: Boolean,
    var isOnFirstRow: Boolean,
    var isEndOfWeek: Boolean,
    val type: CalendarCellType
) {

  companion object {

    private const val WHITE = "white"

    fun empty() = CalendarCell("", 1, 1, WHITE, true, false, false, EMPTY)

    fun merge() = CalendarCell("", 1, 1, WHITE, true, false, false, MERGE)

    fun forMilestone(milestoneCell: MilestoneCell) =
        CalendarCell(milestoneCell, 4, 1, milestoneCell.craftColor, true, false, false, MILESTONE)

    fun forTask(taskCell: TaskCell, duration: Int) =
        CalendarCell(taskCell, duration, 1, taskCell.craftColor, true, false, false, TASK)

    fun blockerFor(cell: CalendarCell) =
        CalendarCell("Blocker for $cell", 1, 1, WHITE, false, false, false, BLOCKER)

    fun forRowHeader(row: RowCell, height: Int) =
        if (row.type == RowCellType.GLOBAL_MILESTONE) {
          forMilestoneHeader(row, height)
        } else {
          forWorkAreaHeader(row, height)
        }

    private fun forWorkAreaHeader(row: RowCell, height: Int) =
        CalendarCell(row, 1, height, WHITE, true, false, false, WORK_AREA_HEADER)

    private fun forMilestoneHeader(row: RowCell, height: Int) =
        CalendarCell(row, 1, height, WHITE, true, false, false, MILESTONE_HEADER)
  }
}

enum class CalendarCellType {
  MERGE,
  EMPTY,
  BLOCKER,
  MILESTONE,
  MILESTONE_HEADER,
  TASK,
  WORK_AREA_HEADER;

  val isBlocker: Boolean
    get() = this == BLOCKER
  val isEmpty: Boolean
    get() = this == EMPTY
  val isMerge: Boolean
    get() = this == MERGE
  val isMilestone: Boolean
    get() = this == MILESTONE
  val isMilestoneHeader: Boolean
    get() = this == MILESTONE_HEADER
  val isTask: Boolean
    get() = this == TASK
  val isWorkAreaHeader: Boolean
    get() = this == WORK_AREA_HEADER
}
