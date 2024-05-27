/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveFirstDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarDateHelper.retrieveLastDayOfWeek
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarRow
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.GridType.WEEK_ROW
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.MilestoneCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.TaskCell
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import org.springframework.stereotype.Component

@Component
class CalendarRowAssembler {

  fun assemble(
      rowCells: List<RowCell>,
      workdayConfiguration: WorkdayConfiguration,
      calendarStart: LocalDate,
      calendarEnd: LocalDate
  ): MutableList<CalendarRow> {
    var repeatWeekHeader = 0
    var addedWeekHeader = false

    val startOfWeek = workdayConfiguration.startOfWeek
    val rows = mutableListOf<CalendarRow>()

    // Calculate the "first day visible in the calendar" and the "last day visible in the calendar"
    // Calculate the length of the calendar in days
    val firstDayVisibleInCalendar = calendarStart.retrieveFirstDayOfWeek(startOfWeek)
    val lastDayVisibleInCalendar = calendarEnd.retrieveLastDayOfWeek(startOfWeek)
    val calendarLengthInDays =
        firstDayVisibleInCalendar.until(lastDayVisibleInCalendar.plusDays(1), DAYS)

    rowCells.sortedWith(ROW_SORT).forEach {
      val milestoneRow =
          assembleMilestoneRow(
              it.milestones,
              firstDayVisibleInCalendar,
              lastDayVisibleInCalendar,
              calendarLengthInDays)

      val taskRow =
          assembleTaskRow(
              it.tasks,
              firstDayVisibleInCalendar,
              lastDayVisibleInCalendar,
              calendarLengthInDays,
              startOfWeek)

      val calendarRow = CalendarRow()
      calendarRow.cells.addAll(milestoneRow.cells)
      calendarRow.cells.addAll(taskRow.cells)

      // insert work area column
      val height = calendarRow.cells.size
      calendarRow.appendCell(0, 0, CalendarCell.forRowHeader(it, height))

      rows.add(calendarRow)

      repeatWeekHeader += taskRow.cells.size

      if (repeatWeekHeader >= REPEATABLE_ROW) {
        repeatWeekHeader = 0
        addedWeekHeader = true
        val weekHeaderCalendarRow = CalendarRow()
        weekHeaderCalendarRow.type = WEEK_ROW
        rows.add(weekHeaderCalendarRow)
      }
    }

    if (!addedWeekHeader) {
      val weekHeaderCalendarRow = CalendarRow()
      weekHeaderCalendarRow.type = WEEK_ROW
      rows.add(weekHeaderCalendarRow)
    }

    return rows
  }

  private fun assembleMilestoneRow(
      milestones: Collection<MilestoneCell>,
      firstDayVisibleInCalendar: LocalDate,
      lastDayVisibleInCalendar: LocalDate,
      calendarLengthInDays: Long
  ): CalendarRow {
    val row = CalendarRow()

    milestones
        .groupByTo(sortedMapOf(LOCAL_DATE_SORT)) { it.date }
        .run {
          this.entries.forEach { milestoneByDateEntry ->
            val startDayOfMilestoneVisibleInCalendar =
                CalendarDateHelper.getStartOfMilestoneVisibleInCalendar(
                    lastDayVisibleInCalendar, milestoneByDateEntry.key)

            /*
             * Calculate the number of days between the "first day visible in the calendar" and
             * the "start day of the milestone visible in the calendar" to use as offset in the grid.
             *
             * This will correspond to the first column where the milestone will be drawn.
             */
            val columnOfStartDay =
                firstDayVisibleInCalendar
                    .until(startDayOfMilestoneVisibleInCalendar, DAYS)
                    .toInt() + 1

            val reverse = milestoneByDateEntry.key != startDayOfMilestoneVisibleInCalendar

            milestoneByDateEntry.value.sortedWith(MILESTONE_DATA_SORT_POSITION).forEach {
              it.inverted = reverse
              row.appendCell(0, columnOfStartDay, CalendarCell.forMilestone(it))
            }
          }
        }

    normalizeGrid(row, calendarLengthInDays)
    return row
  }

  private fun assembleTaskRow(
      tasks: Collection<TaskCell>,
      firstDayVisibleInCalendar: LocalDate,
      lastDayVisibleInCalendar: LocalDate,
      calendarLengthInDays: Long,
      startOfWeek: DayOfWeek
  ): CalendarRow {
    val row = CalendarRow()

    tasks.sortedWith(TASK_DATA_SORT).forEach {
      val startDayOfTaskVisibleInCalendar =
          maxOf(firstDayVisibleInCalendar, it.start.retrieveFirstDayOfWeek(startOfWeek))
      val endDayOfTaskVisibleInCalendar =
          minOf(lastDayVisibleInCalendar, it.end.retrieveLastDayOfWeek(startOfWeek))

      /*
       * Calculate the number of days between the "first day visible in the calendar" and
       * the "start day of the task visible in the calendar" to use as offset in the grid.
       *
       * This will correspond to the first column where the task will be drawn.
       */
      val columnOfStartDay =
          firstDayVisibleInCalendar.until(startDayOfTaskVisibleInCalendar, DAYS).toInt() + 1

      val duration =
          startDayOfTaskVisibleInCalendar
              .until(endDayOfTaskVisibleInCalendar.plusDays(1), DAYS)
              .toInt()

      row.appendCell(0, columnOfStartDay, CalendarCell.forTask(it, duration))
    }

    normalizeGrid(row, calendarLengthInDays)
    row.markCellsForStyling()
    return row
  }

  /*
   * Make the grid a perfect rectangle and merge all empty cells that are vertically connected together.
   */
  private fun normalizeGrid(calendarRow: CalendarRow, calendarLengthInDays: Long) {
    if (calendarRow.cells.isNotEmpty()) {
      calendarRow.makeRectangular(calendarLengthInDays.toInt() + 1)
      calendarRow.mergeEmptyCellsInColumns(calendarRow.cells.size)
    }
  }

  companion object {
    private const val REPEATABLE_ROW = 10

    /*
     * The row data needed to be ordered depending on the case:
     * 1. The global milestones need to come first, the position will be Int.MIN_VALUE
     * 2. The work areas need to be order by the stored position.
     * 3. The row with no work area need to come last, the position will be Int.MAX_VALUE
     */
    private val ROW_SORT = Comparator.comparing(RowCell::position)

    /*
     * The milestone data need to be ordered by date, as well as, for the same date by position.
     * This order will create the correct representation in the calendar.
     */
    private val LOCAL_DATE_SORT = Comparator.naturalOrder<LocalDate>()
    private val MILESTONE_DATA_SORT_POSITION = Comparator.comparing(MilestoneCell::position)

    /*
     * This is the order used to apply in the conversion from the task data to Cells.
     *
     * Note - this order can not be change because it mimics the way the calendar is presented in the web app.
     * It needs to be exactly equal to the task search sort criteria request made from the web app in the calendar.
     */
    private val TASK_DATA_SORT =
        Comparator.comparing(TaskCell::start)
            .thenComparing(TaskCell::company)
            .thenComparing(TaskCell::craftName)
            .thenComparing(TaskCell::name, String.CASE_INSENSITIVE_ORDER)
  }
}
