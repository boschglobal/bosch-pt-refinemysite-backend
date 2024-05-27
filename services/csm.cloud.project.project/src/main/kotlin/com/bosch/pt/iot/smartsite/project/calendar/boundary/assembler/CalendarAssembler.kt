/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler

import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header.MonthRowAssembler
import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header.WeekRowAssembler
import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout.CalendarRowAssembler
import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout.RowCellAssembler
import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.legend.LegendRowAssembler
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarMessageTranslationHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.Calendar
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.legend.LegendRow
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class CalendarAssembler(
    private val legendRowAssembler: LegendRowAssembler,
    private val monthRowAssembler: MonthRowAssembler,
    private val weekRowAssembler: WeekRowAssembler,
    private val rowCellAssembler: RowCellAssembler,
    private val calendarRowAssembler: CalendarRowAssembler,
    private val calendarMessageTranslationHelper: CalendarMessageTranslationHelper
) {

  fun assemble(
      project: Project,
      workdayConfiguration: WorkdayConfiguration,
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      tasks: Collection<Task>,
      constraintSelections: Collection<TaskConstraintSelectionDto>,
      schedules: Collection<TaskScheduleWithDayCardsDto>,
      milestones: Collection<Milestone>,
      criticalMilestoneRelations: Collection<Relation>,
      hasFiltersApplied: Boolean,
      includeDayCards: Boolean,
      includeMilestones: Boolean
  ): Calendar {
    val rowCells =
        rowCellAssembler.assemble(
            calendarStart,
            calendarEnd,
            tasks,
            constraintSelections,
            schedules,
            milestones,
            criticalMilestoneRelations,
            workdayConfiguration,
            includeDayCards,
            includeMilestones)

    return Calendar(
        project.getDisplayName(),
        assembleExportDate(hasFiltersApplied),
        calendarMessageTranslationHelper.getCalendarLegendEmptyName(),
        legendRowAssembler.assemble(tasks, milestones),
        monthRowAssembler.assemble(calendarStart, calendarEnd, workdayConfiguration),
        weekRowAssembler.assemble(
            calendarStart, calendarEnd, workdayConfiguration, includeDayCards, includeMilestones),
        calendarRowAssembler.assemble(rowCells, workdayConfiguration, calendarStart, calendarEnd),
        includeDayCards,
        includeMilestones)
  }

  fun assembleEmpty(
      project: Project,
      workdayConfiguration: WorkdayConfiguration,
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      hasFiltersApplied: Boolean,
      includeDayCards: Boolean,
      includeMilestones: Boolean
  ) =
      Calendar(
          project.getDisplayName(),
          assembleExportDate(hasFiltersApplied),
          calendarMessageTranslationHelper.getCalendarLegendEmptyName(),
          LegendRow(emptyList(), emptyList()),
          monthRowAssembler.assemble(calendarStart, calendarEnd, workdayConfiguration),
          weekRowAssembler.assemble(
              calendarStart, calendarEnd, workdayConfiguration, includeDayCards, includeMilestones),
          emptyList(),
          includeDayCards,
          includeMilestones)

  private fun assembleExportDate(hasFiltersApplied: Boolean): String {
    var exportDate = calendarMessageTranslationHelper.getCalendarExportName()

    if (hasFiltersApplied) {
      exportDate += " / ⚠ " + calendarMessageTranslationHelper.getCalendarFilterAppliedName()
    }

    return exportDate
  }
}
