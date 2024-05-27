/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.BLOCKER
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.MERGE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.MILESTONE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.MILESTONE_HEADER
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.TASK
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.WORK_AREA_HEADER
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.GridType.WEEK_ROW
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.GridType.WORK_AREA_ROW
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.MilestoneCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.GLOBAL_MILESTONE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.WITH_NO_WORK_AREA
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.WORK_AREA
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.TaskCell
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import java.time.LocalDate
import kotlin.Int.Companion.MAX_VALUE
import kotlin.Int.Companion.MIN_VALUE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CalendarRowAssemblerTest {

  private val cut = CalendarRowAssembler()

  @Test
  fun `verify creation of rows with only task cells`() {
    val taskCell1 = buildTaskCell("Task1")
    val taskCell2 = buildTaskCell("Task2")
    val rowCell1 = RowCell("WorkArea1", listOf(taskCell1, taskCell2), emptyList(), 1, WORK_AREA)

    val rows = cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(2)
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 1).content).isEqualTo(taskCell1)
    assertThat(rows[0].cellAt(1, 1).content).isEqualTo(taskCell2)
  }

  @Test
  fun `verify creation of rows with only milestone cells`() {
    val milestoneCell1 = buildMilestoneCell(name = "Milestone1", position = 0)
    val milestoneCell2 = buildMilestoneCell(name = "Milestone2", position = 1)
    val rowCell1 =
        RowCell("WorkArea1", emptyList(), listOf(milestoneCell1, milestoneCell2), 0, WORK_AREA)

    val rows = cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(2)
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 1).content).isEqualTo(milestoneCell1)
    assertThat(rows[0].cellAt(1, 1).content).isEqualTo(milestoneCell2)
  }

  @Test
  fun `verify creation of a rows with milestone and task cells`() {
    val taskCell1 = buildTaskCell("Task1")
    val taskCell2 = buildTaskCell("Task2")
    val milestoneCell1 = buildMilestoneCell(name = "Milestone1", position = 0)
    val milestoneCell2 = buildMilestoneCell(name = "Milestone2", position = 1)
    val rowCell1 =
        RowCell(
            "WorkArea1",
            listOf(taskCell1, taskCell2),
            listOf(milestoneCell1, milestoneCell2),
            0,
            WORK_AREA)

    val rows = cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(4)
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 1).content).isEqualTo(milestoneCell1)
    assertThat(rows[0].cellAt(1, 1).content).isEqualTo(milestoneCell2)
    assertThat(rows[0].cellAt(2, 1).content).isEqualTo(taskCell1)
    assertThat(rows[0].cellAt(3, 1).content).isEqualTo(taskCell2)
  }

  @Test
  fun `verify create a calendar row  always have a minimum of seven days ( one week )`() {
    val startInsideWeek = START_DATE.plusDays(1)
    val endsInsideWeek = START_DATE.plusDays(5)

    val taskCell1 = buildTaskCell("Task1", startInsideWeek.plusDays(1), endsInsideWeek.minusDays(1))
    val rowCell1 = RowCell("WorkArea1", listOf(taskCell1), emptyList(), 1, WORK_AREA)

    val rows =
        cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, startInsideWeek, endsInsideWeek)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(1)

    // Validate the content of the cells
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 1).content).isEqualTo(taskCell1)

    // Validate the length of the calendar is one week plus one day
    // The extra day represents the work area description column
    assertThat(rows[0].cells[0].size).isEqualTo(8)
  }

  @Test
  fun `verify correct placement in the rows of task cells`() {
    val taskCell1 = buildTaskCell("Task1", START_DATE.plusDays(8), END_DATE.minusDays(8))
    val rowCell1 = RowCell("WorkArea1", listOf(taskCell1), emptyList(), 1, WORK_AREA)

    val rows = cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(1)

    // Validate the content of the cells
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 8).content).isEqualTo(taskCell1)

    // Validate the type of the cells
    assertThat(rows[0].cellAt(0, 0).type).isEqualTo(WORK_AREA_HEADER)
    assertThat(rows[0].cellAt(0, 7).type).isEqualTo(MERGE)
    assertThat(rows[0].cellAt(0, 8).type).isEqualTo(TASK)
    assertThat(rows[0].cellAt(0, 9).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 10).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 11).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 12).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 13).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 13).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 14).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 15).type).isEqualTo(MERGE)
  }

  @Test
  fun `verify correct placement in the rows of milestone cells`() {
    val milestoneCell1 =
        buildMilestoneCell(name = "Milestone1", position = 0, start = START_DATE.plusDays(8))
    val rowCell1 =
        RowCell(
            "GlobalMilestones", emptyList(), listOf(milestoneCell1), MIN_VALUE, GLOBAL_MILESTONE)

    val rows = cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(1)

    // Validate the content of the cells
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 9).content).isEqualTo(milestoneCell1)

    // Validate the type of the cells
    assertThat(rows[0].cellAt(0, 0).type).isEqualTo(MILESTONE_HEADER)
    assertThat(rows[0].cellAt(0, 8).type).isEqualTo(MERGE)
    assertThat(rows[0].cellAt(0, 9).type).isEqualTo(MILESTONE)
    assertThat(rows[0].cellAt(0, 10).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 11).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 12).type).isEqualTo(BLOCKER)
  }

  @Test
  fun `verify correct placement in the rows of milestone cells, in the last two days of the calendar`() {
    val milestoneCell1 = buildMilestoneCell(name = "Milestone1", start = END_DATE)
    val milestoneCell2 = buildMilestoneCell(name = "Milestone2", start = END_DATE.minusDays(1))
    val milestoneCell3 = buildMilestoneCell(name = "Milestone3", start = END_DATE.minusDays(2))
    val milestoneCell4 = buildMilestoneCell(name = "Milestone4", start = END_DATE.minusDays(3))
    val rowCell1 =
        RowCell(
            "GlobalMilestone",
            emptyList(),
            listOf(milestoneCell1, milestoneCell2, milestoneCell3, milestoneCell4),
            MIN_VALUE,
            GLOBAL_MILESTONE)

    val rows = cut.assemble(listOf(rowCell1), WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(2)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(4)

    // Validate the content of the cells
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[0].cellAt(0, 18).content).isEqualTo(milestoneCell4)
    assertThat(rows[0].cellAt(1, 16).content).isEqualTo(milestoneCell3)
    assertThat(rows[0].cellAt(2, 17).content).isEqualTo(milestoneCell2)
    assertThat(rows[0].cellAt(3, 18).content).isEqualTo(milestoneCell1)

    // Validate the type of the cells
    assertThat(rows[0].cellAt(0, 0).type).isEqualTo(MILESTONE_HEADER)
    assertThat(rows[0].cellAt(0, 17).type).isEqualTo(MERGE)
    assertThat(rows[0].cellAt(0, 18).type).isEqualTo(MILESTONE)
    assertThat(rows[0].cellAt(0, 19).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 20).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(0, 21).type).isEqualTo(BLOCKER)

    assertThat(rows[0].cellAt(1, 15).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(1, 16).type).isEqualTo(MILESTONE)
    assertThat(rows[0].cellAt(1, 17).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(1, 18).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(1, 19).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(1, 20).type).isEqualTo(MERGE)
    assertThat(rows[0].cellAt(1, 21).type).isEqualTo(MERGE)

    assertThat(rows[0].cellAt(2, 16).type).isEqualTo(MERGE)
    assertThat(rows[0].cellAt(2, 17).type).isEqualTo(MILESTONE)
    assertThat(rows[0].cellAt(2, 18).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(2, 19).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(2, 20).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(2, 21).type).isEqualTo(BLOCKER)

    assertThat(rows[0].cellAt(3, 17).type).isEqualTo(MERGE)
    assertThat(rows[0].cellAt(3, 18).type).isEqualTo(MILESTONE)
    assertThat(rows[0].cellAt(3, 19).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(3, 20).type).isEqualTo(BLOCKER)
    assertThat(rows[0].cellAt(3, 21).type).isEqualTo(BLOCKER)

    // Validate milestones set with inverted to true
    assertThat(milestoneCell1.inverted).isTrue
    assertThat(milestoneCell2.inverted).isTrue
    assertThat(milestoneCell3.inverted).isTrue
    assertThat(milestoneCell4.inverted).isFalse
  }

  @Test
  fun `verify correct placement in the rows of row cells`() {
    val milestoneCell1 = buildMilestoneCell(name = "Milestone1")
    val rowCell1 =
        RowCell("GlobalMilestone", emptyList(), listOf(milestoneCell1), MIN_VALUE, GLOBAL_MILESTONE)

    val taskCell2 = buildTaskCell("Task2")
    val milestoneCell2 = buildMilestoneCell(name = "Milestone2")
    val rowCell2 = RowCell("WorkArea1", listOf(taskCell2), listOf(milestoneCell2), 1, WORK_AREA)

    val taskCell3 = buildTaskCell("Task3")
    val milestoneCell3 = buildMilestoneCell(name = "Milestone3")
    val rowCell3 = RowCell("WorkArea2", listOf(taskCell3), listOf(milestoneCell3), 2, WORK_AREA)

    val taskCell4 = buildTaskCell("Task4")
    val milestoneCell4 = buildMilestoneCell(name = "Milestone4")
    val rowCell4 =
        RowCell(
            "WithoutWorkArea",
            listOf(taskCell4),
            listOf(milestoneCell4),
            MAX_VALUE,
            WITH_NO_WORK_AREA)

    val rows =
        cut.assemble(
            listOf(rowCell1, rowCell2, rowCell3, rowCell4),
            WORKDAY_CONFIGURATION,
            START_DATE,
            END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(5)
    assertThat(rows[0].cellAt(0, 0).height).isEqualTo(1)
    assertThat(rows[0].cellAt(0, 0).content).isEqualTo(rowCell1)
    assertThat(rows[1].cellAt(0, 0).height).isEqualTo(2)
    assertThat(rows[1].cellAt(0, 0).content).isEqualTo(rowCell2)
    assertThat(rows[2].cellAt(0, 0).height).isEqualTo(2)
    assertThat(rows[2].cellAt(0, 0).content).isEqualTo(rowCell3)
    assertThat(rows[3].cellAt(0, 0).height).isEqualTo(2)
    assertThat(rows[3].cellAt(0, 0).content).isEqualTo(rowCell4)
  }

  @Test
  fun `verify correct placement of repeated week row at the end of the calendar with height less than 10`() {
    val taskCell = buildTaskCell("Task")
    val rowCell = RowCell("WorkArea", listOf(taskCell), emptyList(), 1, WORK_AREA)

    val rows = cut.assemble(MutableList(9) { rowCell }, WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(10)
    assertThat(rows[0].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[1].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[2].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[3].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[4].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[5].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[6].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[7].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[8].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[9].type).isEqualTo(WEEK_ROW)
  }

  @Test
  fun `verify correct placement of repeated week row at the end of the calendar with height more than 10`() {
    val taskCell = buildTaskCell("Task")
    val rowCell = RowCell("WorkArea", listOf(taskCell), emptyList(), 1, WORK_AREA)

    val rows =
        cut.assemble(MutableList(10) { rowCell }, WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(11)
    assertThat(rows[0].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[1].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[2].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[3].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[4].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[5].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[6].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[7].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[8].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[9].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[10].type).isEqualTo(WEEK_ROW)
  }

  @Test
  fun `verify correct placement of repeated week row in the middle of the calendar`() {
    val taskCell = buildTaskCell("Task")
    val rowCell = RowCell("WorkArea", listOf(taskCell), emptyList(), 1, WORK_AREA)

    val rows =
        cut.assemble(MutableList(15) { rowCell }, WORKDAY_CONFIGURATION, START_DATE, END_DATE)

    assertThat(rows).isNotNull
    assertThat(rows).hasSize(16)
    assertThat(rows[0].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[1].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[2].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[3].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[4].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[5].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[6].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[7].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[8].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[9].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[10].type).isEqualTo(WEEK_ROW)
    assertThat(rows[11].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[12].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[13].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[14].type).isEqualTo(WORK_AREA_ROW)
    assertThat(rows[15].type).isEqualTo(WORK_AREA_ROW)
  }

  companion object {
    private val START_DATE = LocalDate.of(2022, 1, 5)
    private val END_DATE = START_DATE.plusDays(20)
    private val WORKDAY_CONFIGURATION = buildWorkdayConfiguration()

    fun buildTaskCell(name: String, start: LocalDate = START_DATE, end: LocalDate = END_DATE) =
        TaskCell(name, "Company", "CraftName", "CraftColor", OPEN, start, end, false, emptyList())

    fun buildMilestoneCell(
        name: String,
        type: MilestoneTypeEnum = PROJECT,
        position: Int = 0,
        start: LocalDate = START_DATE
    ) = MilestoneCell(name, "CraftColor", type, position, start)
  }
}
