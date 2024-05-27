/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.Calendar
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildProject
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import java.time.LocalDate
import java.util.Locale
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import java.util.Locale.UK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder.setLocale

@SmartSiteSpringBootTest
class CalendarAssemblerTest {

  @Autowired private lateinit var cut: CalendarAssembler

  @Test
  fun `verify creation of calendar`() {
    val calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.projectName).isNotBlank
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.legendRow).isNotNull
    assertThat(calendar.monthRow).isNotNull
    assertThat(calendar.weekRow).isNotNull
    assertThat(calendar.rows).isNotNull
    assertThat(calendar.includeDayCards).isTrue
    assertThat(calendar.includeMilestones).isTrue
  }

  @Test
  fun `verify creation of calendar with project name`() {
    val calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.projectName).isNotBlank
    assertThat(calendar.projectName).isEqualTo("title")
  }

  @Test
  fun `verify creation of calendar export date with filter`() {
    setLocale(UK)
    var calendar: Calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Export date:")
    assertThat(calendar.exportDate).endsWith("/ ⚠ Filters applied")

    setLocale(GERMANY)
    calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Ausdruckdatum:")
    assertThat(calendar.exportDate).endsWith("/ ⚠ Gefilterte Ansicht")

    setLocale(Locale("es", "ES"))
    calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Fecha impresa:")
    assertThat(calendar.exportDate).endsWith("/ ⚠ Vista filtrada")

    setLocale(FRANCE)
    calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Date d'impression:")
    assertThat(calendar.exportDate).endsWith("/ ⚠ Vue filtrée")

    setLocale(Locale("pt", "PT"))
    calendar = executeCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Data da impressão:")
    assertThat(calendar.exportDate).endsWith("/ ⚠ Visualização filtrada")
  }

  @Test
  fun `verify creation of calendar export date without filter`() {
    setLocale(UK)
    var calendar: Calendar = executeCalendarAssemble(false)

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Export date:")
    assertThat(calendar.exportDate).doesNotContain("/ ⚠ Filters applied")

    setLocale(GERMANY)
    calendar = executeCalendarAssemble(false)

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Ausdruckdatum:")
    assertThat(calendar.exportDate).doesNotContain("/ ⚠ Gefilterte Ansicht")

    setLocale(Locale("es", "ES"))
    calendar = executeCalendarAssemble(false)

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Fecha impresa:")
    assertThat(calendar.exportDate).doesNotContain("/ ⚠ Vista filtrada")

    setLocale(FRANCE)
    calendar = executeCalendarAssemble(false)

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Date d'impression:")
    assertThat(calendar.exportDate).doesNotContain("/ ⚠ Vue filtrée")

    setLocale(Locale("pt", "PT"))
    calendar = executeCalendarAssemble(false)

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.exportDate).startsWith("Data da impressão:")
    assertThat(calendar.exportDate).doesNotContain("/ ⚠ Visualização filtrada")
  }

  @Test
  fun `verify creation of empty calendar`() {
    setLocale(UK)
    var calendar: Calendar = executeEmptyCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.calendarEmptyMessage).startsWith("Exported calendar without content")
    assertThat(calendar.legendRow).isNotNull
    assertThat(calendar.legendRow.crafts).isEmpty()
    assertThat(calendar.legendRow.milestones).isEmpty()
    assertThat(calendar.monthRow).isNotNull
    assertThat(calendar.weekRow).isNotNull
    assertThat(calendar.rows).isNotNull
    assertThat(calendar.rows).isEmpty()
    assertThat(calendar.includeDayCards).isFalse
    assertThat(calendar.includeMilestones).isFalse

    setLocale(GERMANY)
    calendar = executeEmptyCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.calendarEmptyMessage)
        .startsWith("Der exportierte Kalender hat keinen Inhalt")
    assertThat(calendar.legendRow).isNotNull
    assertThat(calendar.legendRow.crafts).isEmpty()
    assertThat(calendar.legendRow.milestones).isEmpty()
    assertThat(calendar.monthRow).isNotNull
    assertThat(calendar.weekRow).isNotNull
    assertThat(calendar.rows).isNotNull
    assertThat(calendar.rows).isEmpty()
    assertThat(calendar.includeDayCards).isFalse
    assertThat(calendar.includeMilestones).isFalse

    setLocale(Locale("es", "ES"))
    calendar = executeEmptyCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.calendarEmptyMessage).startsWith("Calendario exportado sin contenido")
    assertThat(calendar.legendRow).isNotNull
    assertThat(calendar.legendRow.crafts).isEmpty()
    assertThat(calendar.legendRow.milestones).isEmpty()
    assertThat(calendar.monthRow).isNotNull
    assertThat(calendar.weekRow).isNotNull
    assertThat(calendar.rows).isNotNull
    assertThat(calendar.rows).isEmpty()
    assertThat(calendar.includeDayCards).isFalse
    assertThat(calendar.includeMilestones).isFalse

    setLocale(FRANCE)
    calendar = executeEmptyCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.calendarEmptyMessage).startsWith("Le calendrier exporté n'a pas de contenu")
    assertThat(calendar.legendRow).isNotNull
    assertThat(calendar.legendRow.crafts).isEmpty()
    assertThat(calendar.legendRow.milestones).isEmpty()
    assertThat(calendar.monthRow).isNotNull
    assertThat(calendar.weekRow).isNotNull
    assertThat(calendar.rows).isNotNull
    assertThat(calendar.rows).isEmpty()
    assertThat(calendar.includeDayCards).isFalse
    assertThat(calendar.includeMilestones).isFalse

    setLocale(Locale("pt", "PT"))
    calendar = executeEmptyCalendarAssemble()

    assertThat(calendar).isNotNull
    assertThat(calendar.exportDate).isNotBlank
    assertThat(calendar.calendarEmptyMessage).startsWith("Calendário exportado sem conteúdo")
    assertThat(calendar.legendRow).isNotNull
    assertThat(calendar.legendRow.crafts).isEmpty()
    assertThat(calendar.legendRow.milestones).isEmpty()
    assertThat(calendar.monthRow).isNotNull
    assertThat(calendar.weekRow).isNotNull
    assertThat(calendar.rows).isNotNull
    assertThat(calendar.rows).isEmpty()
    assertThat(calendar.includeDayCards).isFalse
    assertThat(calendar.includeMilestones).isFalse
  }

  private fun executeCalendarAssemble(hasFilterApplied: Boolean = true): Calendar {
    val project = buildProject()
    val workdayConfiguration = buildWorkdayConfiguration { it.project = project }

    return cut.assemble(
        project,
        workdayConfiguration,
        START_DATE,
        END_DATE,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        hasFilterApplied,
        includeDayCards = true,
        includeMilestones = true)
  }

  private fun executeEmptyCalendarAssemble(): Calendar {
    val project = buildProject()
    val workdayConfiguration = buildWorkdayConfiguration { it.project = project }

    return cut.assembleEmpty(
        project,
        workdayConfiguration,
        START_DATE,
        END_DATE,
        hasFiltersApplied = false,
        includeDayCards = false,
        includeMilestones = false)
  }

  companion object {
    private val START_DATE = LocalDate.of(2019, 10, 7)
    private val END_DATE = START_DATE.plusDays(6)
  }
}
