/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarMessageTranslationHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.MilestoneCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.GLOBAL_MILESTONE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.WITH_NO_WORK_AREA
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.WORK_AREA
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.TaskCell
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import kotlin.Int.Companion.MAX_VALUE
import kotlin.Int.Companion.MIN_VALUE
import org.springframework.stereotype.Component

@Component
class RowCellAssembler(
    private val taskCellAssembler: TaskCellAssembler,
    private val milestoneCellAssembler: MilestoneCellAssembler,
    private val calendarMessageTranslationHelper: CalendarMessageTranslationHelper
) {

  fun assemble(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      tasks: Collection<Task>,
      constraintSelections: Collection<TaskConstraintSelectionDto>,
      schedules: Collection<TaskScheduleWithDayCardsDto>,
      milestones: Collection<Milestone>,
      criticalMilestoneRelations: Collection<Relation>,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean,
      includeMilestones: Boolean
  ): List<RowCell> {
    val rowCells = mutableListOf<RowCell>()

    val milestoneCellsWithHeader =
        assembleMilestoneCellsWithHeader(milestones, criticalMilestoneRelations, includeMilestones)

    val workAreaToTaskCells =
        assembleTaskCellsGroupedByWorkArea(
            calendarStart,
            calendarEnd,
            tasks,
            constraintSelections,
            schedules,
            workdayConfiguration,
            includeDayCards)

    val workAreaToMilestoneCells =
        assembleMilestoneCellsGroupedByWorkArea(
            milestones, criticalMilestoneRelations, includeMilestones)

    val taskCellsWithNoWorkArea =
        assembleTaskCellsWithNoWorkArea(
            calendarStart,
            calendarEnd,
            tasks,
            constraintSelections,
            schedules,
            workdayConfiguration,
            includeDayCards)

    val milestoneCellsWithNoWorkArea =
        assembleMilestoneCellsWithNoWorkArea(
            milestones, criticalMilestoneRelations, includeMilestones)

    // Create the row data for the representation of the global milestones.
    // Note - The global milestones row should only be added if there is any data to be represented.
    if (isAnyElementForHeaderVisible(milestoneCellsWithHeader, includeMilestones)) {
      rowCells.add(assembleRowCellForMilestoneCellsWithHeader(milestoneCellsWithHeader))
    }

    // Create the list of rows data for the representation of the work areas.
    rowCells.addAll(assembleRowForWorkAreas(workAreaToTaskCells, workAreaToMilestoneCells))

    // Does the same as the code above but for a special with no work area
    // to accommodate all the tasks and milestones that don't have work area assigned.
    if (isAnyElementForNoWorkAreaVisible(taskCellsWithNoWorkArea, milestoneCellsWithNoWorkArea)) {
      rowCells.add(
          assembleRowForWithoutWorkArea(taskCellsWithNoWorkArea, milestoneCellsWithNoWorkArea))
    }

    return rowCells
  }

  private fun assembleMilestoneCellsWithHeader(
      milestones: Collection<Milestone>,
      criticalMilestoneRelations: Collection<Relation>,
      includeMilestones: Boolean
  ) =
      if (isAnyMilestonePossibleToAssemble(milestones, includeMilestones))
          milestoneCellAssembler.assemble(
              milestones.filter { it.hasHeader() }, criticalMilestoneRelations)
      else emptyList()

  private fun assembleTaskCellsGroupedByWorkArea(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      tasks: Collection<Task>,
      constraintSelections: Collection<TaskConstraintSelectionDto>,
      schedules: Collection<TaskScheduleWithDayCardsDto>,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean
  ) =
      if (tasks.isNotEmpty())
          tasks
              .filter { it.containsWorkArea() }
              .groupBy { it.workArea!! }
              .mapValues {
                taskCellAssembler.assemble(
                    calendarStart,
                    calendarEnd,
                    it.value,
                    schedules,
                    constraintSelections,
                    workdayConfiguration,
                    includeDayCards)
              }
      else emptyMap()

  private fun assembleMilestoneCellsGroupedByWorkArea(
      milestones: Collection<Milestone>,
      criticalMilestoneRelations: Collection<Relation>,
      includeMilestones: Boolean
  ) =
      if (isAnyMilestonePossibleToAssemble(milestones, includeMilestones))
          milestones
              .filter { it.containsWorkArea() }
              .groupBy { it.workArea!! }
              .mapValues { milestoneCellAssembler.assemble(it.value, criticalMilestoneRelations) }
      else emptyMap()

  private fun assembleTaskCellsWithNoWorkArea(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      tasks: Collection<Task>,
      constraintSelections: Collection<TaskConstraintSelectionDto>,
      schedules: Collection<TaskScheduleWithDayCardsDto>,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean
  ) =
      if (tasks.isNotEmpty())
          taskCellAssembler.assemble(
              calendarStart,
              calendarEnd,
              tasks.filterNot { it.containsWorkArea() },
              schedules,
              constraintSelections,
              workdayConfiguration,
              includeDayCards)
      else emptyList()

  private fun assembleMilestoneCellsWithNoWorkArea(
      milestones: Collection<Milestone>,
      criticalMilestoneRelations: Collection<Relation>,
      includeMilestones: Boolean
  ) =
      if (isAnyMilestonePossibleToAssemble(milestones, includeMilestones))
          milestoneCellAssembler.assemble(
              milestones.filterNot { it.containsWorkArea() }.filterNot { it.hasHeader() },
              criticalMilestoneRelations)
      else emptyList()

  private fun assembleRowCellForMilestoneCellsWithHeader(milestones: Collection<MilestoneCell>) =
      RowCell(
          calendarMessageTranslationHelper.getGlobalMilestoneName(),
          emptyList(),
          milestones,
          MIN_VALUE,
          GLOBAL_MILESTONE)

  private fun assembleRowForWorkAreas(
      workAreaToTaskCell: Map<WorkArea, List<TaskCell>>,
      workAreaToMilestoneCell: Map<WorkArea, List<MilestoneCell>>
  ) =
      (workAreaToTaskCell.keys + workAreaToMilestoneCell.keys).distinct().map {
        RowCell(
          it.name,
            workAreaToTaskCell[it].orEmpty(),
            workAreaToMilestoneCell[it].orEmpty(),
            it.position!!,
            WORK_AREA)
      }

  private fun assembleRowForWithoutWorkArea(
      taskData: Collection<TaskCell>,
      milestoneData: Collection<MilestoneCell>
  ) =
      RowCell(
          calendarMessageTranslationHelper.getNoWorkAreaName(),
          taskData,
          milestoneData,
          MAX_VALUE,
          WITH_NO_WORK_AREA)

  private fun isAnyMilestonePossibleToAssemble(
      milestones: Collection<Milestone>,
      includeMilestones: Boolean
  ) = includeMilestones && milestones.isNotEmpty()

  private fun isAnyElementForHeaderVisible(
      milestoneCellsWithHeader: Collection<MilestoneCell>,
      includeMilestones: Boolean
  ) = includeMilestones && milestoneCellsWithHeader.isNotEmpty()

  private fun isAnyElementForNoWorkAreaVisible(
      taskCellsWithNoWorkArea: Collection<TaskCell>,
      milestoneCellsWithNoWorkArea: Collection<MilestoneCell>
  ) = taskCellsWithNoWorkArea.isNotEmpty() || milestoneCellsWithNoWorkArea.isNotEmpty()

  private fun Task.containsWorkArea() = this.workArea != null

  private fun Milestone.containsWorkArea() = this.workArea != null

  private fun Milestone.hasHeader() = this.header
}
