/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.TaskCell
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class TaskCellAssembler(private val dayCardCellAssembler: DayCardCellAssembler) {

  fun assemble(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      tasks: Collection<Task>,
      schedulesWithDayCards: Collection<TaskScheduleWithDayCardsDto>,
      constraintSelections: Collection<TaskConstraintSelectionDto>,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean
  ): List<TaskCell> {
    val taskWithConstraints = getTaskIdentifiersFromConstraintSelections(constraintSelections)
    val taskToDayCardCells =
        dayCardCellsGroupedByTask(
            calendarStart,
            calendarEnd,
            tasks,
            schedulesWithDayCards,
            workdayConfiguration,
            includeDayCards)

    return tasks.map {
      TaskCell(
        it.name,
          it.assignee?.company?.name ?: NO_PLACEHOLDER,
          it.projectCraft.name,
          it.projectCraft.color,
        it.status,
          it.taskSchedule!!.start!!,
          it.taskSchedule!!.end!!,
          taskWithConstraints.contains(it.identifier),
          taskToDayCardCells[it].orEmpty())
    }
  }

  private fun getTaskIdentifiersFromConstraintSelections(
      taskConstraintSelections: Collection<TaskConstraintSelectionDto>
  ) =
      taskConstraintSelections
          .filter { it.constraints.isNotEmpty() }
          .map { it.taskIdentifier }
          .toSet()

  /*
   * This function return a map with a list of DayCardCell for each corresponding task.
   * The range of DayCarCells is between the "start of the week of the task visible in
   * the calendar" and the "end of the week of the task visible in the calendar".
   *
   * Refer to Miro Calendar PDF Export for a visual examples.
   */
  private fun dayCardCellsGroupedByTask(
      calendarStart: LocalDate,
      calendarEnd: LocalDate,
      tasks: Collection<Task>,
      schedulesWithDayCards: Collection<TaskScheduleWithDayCardsDto>,
      workdayConfiguration: WorkdayConfiguration,
      includeDayCards: Boolean
  ): Map<Task, List<DayCardCell>> {
    if (includeDayCards.not()) {
      return emptyMap()
    }

    check(
        schedulesWithDayCards.map { it.taskIdentifier }.containsAll(tasks.map { it.identifier })) {
          "There are missing schedules for the given tasks."
        }

    val taskIdentifierToScheduleWithDayCards =
        schedulesWithDayCards.associateBy { it.taskIdentifier }

    return tasks.associateWith {
      dayCardCellAssembler.assemble(
          taskIdentifierToScheduleWithDayCards[it.identifier]!!,
          calendarStart,
          calendarEnd,
          workdayConfiguration)
    }
  }

  companion object {
    private const val NO_PLACEHOLDER = "---"
  }
}
