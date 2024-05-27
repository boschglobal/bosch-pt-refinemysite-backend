/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCellType.BLANK
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCellType.DAYCARD
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.DayCardCellType.OUT_OF_SCHEDULE
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTaskScheduleWithoutDayCardsDto
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.APPROVED
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.NOTDONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import java.math.BigDecimal
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DayCardCellAssemblerTest {

  private val cut = DayCardCellAssembler()

  @Test
  fun `verify creation of cell for a day card`() {
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(start = START_DATE, end = END_DATE),
            listOf(buildTaskScheduleSlotWithDayCardDto(date = START_DATE)))

    val dayCardCells = cut.assemble(schedule, START_DATE, END_DATE, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].name).isEqualTo("DayCard")
    assertThat(dayCardCells[0].manpower).isEqualTo("3.5")
    assertThat(dayCardCells[0].status).isEqualTo(OPEN)
    assertThat(dayCardCells[0].type).isEqualTo(DAYCARD)
  }

  @Test
  fun `verify creation of cell for a day card in correct order`() {
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(start = START_DATE, end = END_DATE),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE, "DayCardTitle0", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.plusDays(1), "DayCardTitle1", BigDecimal("5"), NOTDONE),
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.plusDays(2), "DayCardTitle2", BigDecimal("4.0"), OPEN)))

    val dayCardCells = cut.assemble(schedule, START_DATE, END_DATE, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].name).isEqualTo("DayCardTitle0")
    assertThat(dayCardCells[1].name).isEqualTo("DayCardTitle1")
    assertThat(dayCardCells[2].name).isEqualTo("DayCardTitle2")
  }

  @Test
  fun `verify creation of cells for a task with blank slots`() {
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(START_DATE.plusDays(1), START_DATE.plusDays(5)),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.plusDays(1), "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.plusDays(5), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, START_DATE, END_DATE, WORKDAY_CONFIGURATION)

    // Check that the day cards entry inside the third, four and five day of the task is BLANK
    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[1].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[2].type).isEqualTo(BLANK)
    assertThat(dayCardCells[3].type).isEqualTo(BLANK)
    assertThat(dayCardCells[4].type).isEqualTo(BLANK)
    assertThat(dayCardCells[5].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[6].type).isEqualTo(OUT_OF_SCHEDULE)
  }

  @Test
  fun `verify creation of cells for a task with range smaller that the export timespan`() {
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(START_DATE.plusDays(1), START_DATE.plusDays(2)),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.plusDays(1), "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.plusDays(2), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, START_DATE, END_DATE, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    // Check that the day cards entry before the task start date are OUT_OF_DATE entries
    assertThat(dayCardCells[0].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[1].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[2].type).isEqualTo(DAYCARD)
    // Check that the day cards entry after the task start date are OUT_OF_DATE entries
    assertThat(dayCardCells[3].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[4].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[5].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[6].type).isEqualTo(OUT_OF_SCHEDULE)
  }

  @Test
  fun `verify creation of cells for a task range bigger that the export timespan`() {
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(START_DATE.minusDays(6), END_DATE.plusDays(6)),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    START_DATE.minusDays(6), "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    END_DATE.plusDays(6), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, START_DATE, END_DATE, WORKDAY_CONFIGURATION)

    // Check that the day cards size corresponds of 7 days (part of task inside timespan of the
    // calendar)
    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(BLANK)
    assertThat(dayCardCells[1].type).isEqualTo(BLANK)
    assertThat(dayCardCells[2].type).isEqualTo(BLANK)
    assertThat(dayCardCells[3].type).isEqualTo(BLANK)
    assertThat(dayCardCells[4].type).isEqualTo(BLANK)
    assertThat(dayCardCells[5].type).isEqualTo(BLANK)
    assertThat(dayCardCells[6].type).isEqualTo(BLANK)
  }

  @Test
  fun `verify creation of cells with task contained inside two weeks`() {
    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(START_DATE.plusDays(6), START_DATE.plusDays(7)),
            emptyList())

    val dayCardCells =
        cut.assemble(schedule, START_DATE, START_DATE.plusDays(13), WORKDAY_CONFIGURATION)

    // Check that the day cards size corresponds of 14 days (the task is the complete timespan of
    // the calendar)
    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(14)
  }

  // Case 1 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when task start date smaller than export start date in same week`() {
    val exportStart = LocalDate.of(2022, 1, 19)
    val exportEnd = LocalDate.of(2022, 1, 23)

    val taskStart = LocalDate.of(2022, 1, 18)
    val taskEnd = LocalDate.of(2022, 1, 23)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart.plusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[1].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[2].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[3].type).isEqualTo(BLANK)
    assertThat(dayCardCells[4].type).isEqualTo(BLANK)
    assertThat(dayCardCells[5].type).isEqualTo(BLANK)
    assertThat(dayCardCells[6].type).isEqualTo(BLANK)
  }

  // Case 2 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when task start date smaller than export start date in different weeks`() {
    val exportStart = LocalDate.of(2022, 1, 25)
    val exportEnd = LocalDate.of(2022, 1, 30)

    val taskStart = LocalDate.of(2022, 1, 18)
    val taskEnd = LocalDate.of(2022, 1, 24)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart.plusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(BLANK)
    assertThat(dayCardCells[1].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[2].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[3].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[4].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[5].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[6].type).isEqualTo(OUT_OF_SCHEDULE)
  }

  // Case 3 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when export start date smaller than task start date in same weeks`() {
    val exportStart = LocalDate.of(2022, 1, 18)
    val exportEnd = LocalDate.of(2022, 1, 23)

    val taskStart = LocalDate.of(2022, 1, 19)
    val taskEnd = LocalDate.of(2022, 1, 23)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart.plusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[1].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[2].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[3].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[4].type).isEqualTo(BLANK)
    assertThat(dayCardCells[5].type).isEqualTo(BLANK)
    assertThat(dayCardCells[6].type).isEqualTo(BLANK)
  }

  // Case 4 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when export start date smaller than task start date in different weeks`() {
    val exportStart = LocalDate.of(2022, 1, 18)
    val exportEnd = LocalDate.of(2022, 1, 30)

    val taskStart = LocalDate.of(2022, 1, 25)
    val taskEnd = LocalDate.of(2022, 1, 30)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskStart.plusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[1].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[2].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[3].type).isEqualTo(BLANK)
    assertThat(dayCardCells[4].type).isEqualTo(BLANK)
    assertThat(dayCardCells[5].type).isEqualTo(BLANK)
    assertThat(dayCardCells[6].type).isEqualTo(BLANK)
  }

  // Case 5 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when task end date bigger than export end date in same week`() {
    val exportStart = LocalDate.of(2022, 1, 24)
    val exportEnd = LocalDate.of(2022, 1, 28)

    val taskStart = LocalDate.of(2022, 1, 24)
    val taskEnd = LocalDate.of(2022, 1, 29)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd.minusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(BLANK)
    assertThat(dayCardCells[1].type).isEqualTo(BLANK)
    assertThat(dayCardCells[2].type).isEqualTo(BLANK)
    assertThat(dayCardCells[3].type).isEqualTo(BLANK)
    assertThat(dayCardCells[4].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[5].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[6].type).isEqualTo(OUT_OF_SCHEDULE)
  }

  // Case 6 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when task end date bigger than export end date in different weeks`() {
    val exportStart = LocalDate.of(2022, 1, 17)
    val exportEnd = LocalDate.of(2022, 1, 22)

    val taskStart = LocalDate.of(2022, 1, 23)
    val taskEnd = LocalDate.of(2022, 1, 29)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd.minusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[1].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[2].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[3].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[4].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[5].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[6].type).isEqualTo(BLANK)
  }

  // Case 7 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when export end date bigger than task end date in same week`() {
    val exportStart = LocalDate.of(2022, 1, 24)
    val exportEnd = LocalDate.of(2022, 1, 29)

    val taskStart = LocalDate.of(2022, 1, 24)
    val taskEnd = LocalDate.of(2022, 1, 28)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd.minusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(BLANK)
    assertThat(dayCardCells[1].type).isEqualTo(BLANK)
    assertThat(dayCardCells[2].type).isEqualTo(BLANK)
    assertThat(dayCardCells[3].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[4].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[5].type).isEqualTo(OUT_OF_SCHEDULE)
    assertThat(dayCardCells[6].type).isEqualTo(OUT_OF_SCHEDULE)
  }

  // Case 8 of Miro - Calendar PDF Export
  @Test
  fun `verify creation of cells when export end date bigger than task end date in different weeks`() {
    val exportStart = LocalDate.of(2022, 1, 17)
    val exportEnd = LocalDate.of(2022, 1, 29)

    val taskStart = LocalDate.of(2022, 1, 17)
    val taskEnd = LocalDate.of(2022, 1, 22)

    val schedule =
        TaskScheduleWithDayCardsDto(
            buildTaskScheduleWithoutDayCardsDto(taskStart, taskEnd),
            listOf(
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd, "DayCardTitle1", BigDecimal("3.50"), APPROVED),
                buildTaskScheduleSlotWithDayCardDto(
                    taskEnd.minusDays(1), "DayCardTitle2", BigDecimal("5"), NOTDONE)))

    val dayCardCells = cut.assemble(schedule, exportStart, exportEnd, WORKDAY_CONFIGURATION)

    assertThat(dayCardCells).isNotNull
    assertThat(dayCardCells).hasSize(7)
    assertThat(dayCardCells[0].type).isEqualTo(BLANK)
    assertThat(dayCardCells[1].type).isEqualTo(BLANK)
    assertThat(dayCardCells[2].type).isEqualTo(BLANK)
    assertThat(dayCardCells[3].type).isEqualTo(BLANK)
    assertThat(dayCardCells[4].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[5].type).isEqualTo(DAYCARD)
    assertThat(dayCardCells[6].type).isEqualTo(OUT_OF_SCHEDULE)
  }

  companion object {
    private val START_DATE = LocalDate.of(2019, 10, 7)
    private val END_DATE = START_DATE.plusDays(6)

    // In this case we use the monday as start of week to be consistent with the miro diagrams
    private val WORKDAY_CONFIGURATION = buildWorkdayConfiguration { it.startOfWeek = MONDAY }
  }
}
