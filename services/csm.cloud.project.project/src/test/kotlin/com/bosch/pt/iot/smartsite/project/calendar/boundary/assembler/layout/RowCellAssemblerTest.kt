/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.GLOBAL_MILESTONE
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.WITH_NO_WORK_AREA
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.RowCellType.WORK_AREA
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildMilestone
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildProject
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTask
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import java.time.LocalDate
import java.util.Locale
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import java.util.Locale.UK
import kotlin.Int.Companion.MAX_VALUE
import kotlin.Int.Companion.MIN_VALUE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder.setLocale

@SmartSiteSpringBootTest
class RowCellAssemblerTest {

  @Autowired private lateinit var cut: RowCellAssembler

  @Test
  fun `verify creation of row for milestones with header`() {
    val milestone1 = buildMilestone { it.date = START_DATE.plusDays(1) }
    val milestone2 = buildMilestone { it.date = START_DATE.plusDays(1) }

    setLocale(UK)
    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(milestone1, milestone2),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(1)
    assertThat(rowCells[0].name).isEqualTo("Global milestones")
    assertThat(rowCells[0].tasks).hasSize(0)
    assertThat(rowCells[0].milestones).hasSize(2)
    assertThat(rowCells[0].position).isEqualTo(MIN_VALUE)
    assertThat(rowCells[0].type).isEqualTo(GLOBAL_MILESTONE)
  }

  @Test
  fun `verify creation of row for milestones with work areas`() {
    val milestone1 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
      it.workArea = WORK_AREA_1
    }
    val milestone2 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
      it.workArea = WORK_AREA_2
    }

    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(milestone1, milestone2),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo(WORK_AREA_1.name)
    assertThat(rowCells[0].tasks).hasSize(0)
    assertThat(rowCells[0].milestones).hasSize(1)
    assertThat(rowCells[0].position).isEqualTo(WORK_AREA_1.position)
    assertThat(rowCells[0].type).isEqualTo(WORK_AREA)
    assertThat(rowCells[1].name).isEqualTo(WORK_AREA_2.name)
    assertThat(rowCells[1].tasks).hasSize(0)
    assertThat(rowCells[1].milestones).hasSize(1)
    assertThat(rowCells[1].position).isEqualTo(WORK_AREA_2.position)
    assertThat(rowCells[1].type).isEqualTo(WORK_AREA)
  }

  @Test
  fun `verify creation of row for milestones without work areas`() {
    val milestone1 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
    }
    val milestone2 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
    }

    setLocale(UK)
    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(milestone1, milestone2),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(1)
    assertThat(rowCells[0].name).isEqualTo("Without working area")
    assertThat(rowCells[0].tasks).hasSize(0)
    assertThat(rowCells[0].milestones).hasSize(2)
    assertThat(rowCells[0].position).isEqualTo(MAX_VALUE)
    assertThat(rowCells[0].type).isEqualTo(WITH_NO_WORK_AREA)
  }

  @Test
  fun `verify creation of row for non include milestones `() {
    val milestone1 = buildMilestone { it.date = START_DATE.plusDays(1) }
    val milestone2 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
      it.workArea = WORK_AREA_2
    }
    val milestone3 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
    }

    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(milestone1, milestone2, milestone3),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(0)
  }

  @Test
  fun `verify creation of row for tasks with work areas`() {
    val task1 = buildTask { it.workArea = WORK_AREA_1 }
    val task2 = buildTask { it.workArea = WORK_AREA_2 }

    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1, task2),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo(WORK_AREA_1.name)
    assertThat(rowCells[0].tasks).hasSize(1)
    assertThat(rowCells[0].milestones).hasSize(0)
    assertThat(rowCells[0].position).isEqualTo(WORK_AREA_1.position)
    assertThat(rowCells[0].type).isEqualTo(WORK_AREA)
    assertThat(rowCells[1].name).isEqualTo(WORK_AREA_2.name)
    assertThat(rowCells[1].tasks).hasSize(1)
    assertThat(rowCells[1].milestones).hasSize(0)
    assertThat(rowCells[1].position).isEqualTo(WORK_AREA_2.position)
    assertThat(rowCells[1].type).isEqualTo(WORK_AREA)
  }

  @Test
  fun `verify creation of row for tasks without work areas`() {
    val task1 = buildTask()
    val task2 = buildTask()

    setLocale(UK)
    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1, task2),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(1)
    assertThat(rowCells[0].name).isEqualTo("Without working area")
    assertThat(rowCells[0].tasks).hasSize(2)
    assertThat(rowCells[0].milestones).hasSize(0)
    assertThat(rowCells[0].position).isEqualTo(MAX_VALUE)
    assertThat(rowCells[0].type).isEqualTo(WITH_NO_WORK_AREA)
  }

  @Test
  fun `verify creation of row for tasks and milestones for the same work areas`() {
    val task1 = buildTask { it.workArea = WORK_AREA_1 }
    val milestone1 = buildMilestone {
      it.date = START_DATE.plusDays(1)
      it.header = false
      it.workArea = WORK_AREA_1
    }

    val rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1),
            emptyList(),
            emptyList(),
            listOf(milestone1),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(1)
    assertThat(rowCells[0].name).isEqualTo(WORK_AREA_1.name)
    assertThat(rowCells[0].tasks).hasSize(1)
    assertThat(rowCells[0].milestones).hasSize(1)
    assertThat(rowCells[0].position).isEqualTo(WORK_AREA_1.position)
    assertThat(rowCells[0].type).isEqualTo(WORK_AREA)
  }

  @Test
  fun `verify milestones with header and no work area name translations`() {
    var rowCells: List<RowCell>
    val task1 = buildTask()
    val milestone1 = buildMilestone { it.date = START_DATE.plusDays(1) }

    setLocale(UK)
    rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1),
            emptyList(),
            emptyList(),
            listOf(milestone1),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo("Global milestones")
    assertThat(rowCells[1].name).isEqualTo("Without working area")

    setLocale(GERMANY)
    rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1),
            emptyList(),
            emptyList(),
            listOf(milestone1),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo("Globale Meilensteine")
    assertThat(rowCells[1].name).isEqualTo("ohne Bereich")

    setLocale(Locale("es", "ES"))
    rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1),
            emptyList(),
            emptyList(),
            listOf(milestone1),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo("Hitos generales")
    assertThat(rowCells[1].name).isEqualTo("sin área")

    setLocale(FRANCE)
    rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1),
            emptyList(),
            emptyList(),
            listOf(milestone1),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo("Jalons globaux")
    assertThat(rowCells[1].name).isEqualTo("sans domaine")

    setLocale(Locale("pt", "PT"))
    rowCells =
        cut.assemble(
            START_DATE,
            END_DATE,
            listOf(task1),
            emptyList(),
            emptyList(),
            listOf(milestone1),
            emptyList(),
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(rowCells).isNotNull
    assertThat(rowCells).hasSize(2)
    assertThat(rowCells[0].name).isEqualTo("Metas gerais")
    assertThat(rowCells[1].name).isEqualTo("sem área de trabalho")
  }

  companion object {
    private val START_DATE = LocalDate.of(2022, 1, 5)
    private val END_DATE = START_DATE.plusDays(20)
    private val WORKDAY_CONFIGURATION = buildWorkdayConfiguration()
    private val WORK_AREA_1 = WorkArea(WorkAreaId(), buildProject(), "WorkArea1", 1)
    private val WORK_AREA_2 = WorkArea(WorkAreaId(), buildProject(), "WorkArea2", 2)
  }
}
