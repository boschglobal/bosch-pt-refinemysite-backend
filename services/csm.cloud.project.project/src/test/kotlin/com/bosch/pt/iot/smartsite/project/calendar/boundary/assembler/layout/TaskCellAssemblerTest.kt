/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.END_DATE
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.START_DATE
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildParticipant
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTask
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTaskScheduleWithoutDayCardsDto
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.COMMON_UNDERSTANDING
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TaskCellAssemblerTest {

  private val cut = TaskCellAssembler(DayCardCellAssembler())

  @Test
  fun `verify creation of cell for a task`() {
    val startDate = START_DATE.plusDays(2)
    val endDate = END_DATE.minusDays(2)

    val task = buildTask {
      it.taskSchedule!!.start = startDate
      it.taskSchedule!!.end = endDate
    }

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            emptyList(),
            emptyList(),
            WORKDAY_CONFIGURATION,
            false)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].name).isEqualTo("Task")
    assertThat(taskCells[0].company).isEqualTo("---")
    assertThat(taskCells[0].craftName).isEqualTo("ProjectCraft")
    assertThat(taskCells[0].craftColor).isEqualTo("Black")
    assertThat(taskCells[0].status).isEqualTo(OPEN)
    assertThat(taskCells[0].getStatusIcon()).isEqualTo("#task-status-open")
    assertThat(taskCells[0].start).isEqualTo(startDate)
    assertThat(taskCells[0].end).isEqualTo(endDate)
    assertThat(taskCells[0].hasTaskConstraint).isEqualTo(false)
    assertThat(taskCells[0].dayCards).hasSize(0)
  }

  @Test
  fun `verify creation of cell for a task with assignee`() {
    val task = buildTask { it.assignee = buildParticipant() }

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            emptyList(),
            emptyList(),
            WORKDAY_CONFIGURATION,
            false)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].company).isEqualTo("Company")
  }

  @Test
  fun `verify creation of cell for a task with constraints`() {
    val task = buildTask()
    val taskConstraint =
        TaskConstraintSelectionDto(randomUUID(), 0L, task.identifier, listOf(COMMON_UNDERSTANDING))

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            emptyList(),
            listOf(taskConstraint),
            WORKDAY_CONFIGURATION,
            false)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].hasTaskConstraint).isEqualTo(true)
  }

  @Test
  fun `verify creation of cell for a task with empty set of constraints`() {
    val task = buildTask()
    val taskConstraint = TaskConstraintSelectionDto(randomUUID(), 0L, task.identifier, emptyList())

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            emptyList(),
            listOf(taskConstraint),
            WORKDAY_CONFIGURATION,
            false)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].hasTaskConstraint).isEqualTo(false)
  }

  @Test
  fun `verify creation of cell for a task with no constraints`() {
    val task = buildTask()
    val taskConstraint = TaskConstraintSelectionDto(randomUUID(), 0L, TaskId(), emptyList())

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            emptyList(),
            listOf(taskConstraint),
            WORKDAY_CONFIGURATION,
            false)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].hasTaskConstraint).isEqualTo(false)
  }

  @Test
  fun `verify creation of cell for a task not including daycards`() {
    val task = buildTask()
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskIdentifier = task.identifier), emptyList())

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            listOf(schedule),
            emptyList(),
            WORKDAY_CONFIGURATION,
            false)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].dayCards).hasSize(0)
  }

  @Test
  fun `verify creation of cell for a task including daycards`() {
    val task = buildTask()
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskIdentifier = task.identifier),
            listOf(buildTaskScheduleSlotWithDayCardDto()))

    val taskCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task),
            listOf(schedule),
            emptyList(),
            WORKDAY_CONFIGURATION,
            true)

    assertThat(taskCells).isNotNull
    assertThat(taskCells).hasSize(1)
    assertThat(taskCells[0].dayCards).hasSize(7)
  }

  @Test
  fun `verify creation of cell for a task fails if the schedule is not present`() {
    val task = buildTask()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          cut.assemble(
              START_DATE,
              END_DATE,
              listOf(task),
              emptyList(),
              emptyList(),
              WORKDAY_CONFIGURATION,
              true)
        }

    assertThat(exception.message).isEqualTo("There are missing schedules for the given tasks.")
  }

  companion object {
    private val WORKDAY_CONFIGURATION = buildWorkdayConfiguration()
  }
}
