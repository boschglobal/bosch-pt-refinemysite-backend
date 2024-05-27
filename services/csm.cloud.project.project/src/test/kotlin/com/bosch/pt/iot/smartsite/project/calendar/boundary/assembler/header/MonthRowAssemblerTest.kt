/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import java.time.LocalDate
import java.util.Locale.UK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.context.i18n.LocaleContextHolder.setLocale

class MonthRowAssemblerTest {

  private val cut = MonthRowAssembler(MonthCellAssembler())

  @Test
  fun `verify creation of row with one month`() {
    val startDate = LocalDate.of(2022, 10, 6)
    val endDate = startDate.plusDays(1)

    val monthRow = cut.assemble(startDate, endDate, WORKDAY_CONFIGURATION)

    assertThat(monthRow).isNotNull
    assertThat(monthRow.months).isNotNull
    assertThat(monthRow.months).hasSize(1)
    assertThat(monthRow.months[0].length).isEqualTo(7L)
    assertThat(monthRow.months[0].name).isEqualTo("October")
    assertThat(monthRow.year).isEqualTo(2022)
  }

  @Test
  fun `verify creation of row with crossing months of the same week`() {
    val startDate = LocalDate.of(2022, 10, 27)
    val endDate = startDate.plusDays(1)

    val monthRow = cut.assemble(startDate, endDate, WORKDAY_CONFIGURATION)

    assertThat(monthRow).isNotNull
    assertThat(monthRow.months).isNotNull
    assertThat(monthRow.months).hasSize(2)
    assertThat(monthRow.months[0].length).isEqualTo(6L)
    assertThat(monthRow.months[0].name).isEqualTo("October")
    assertThat(monthRow.months[1].length).isEqualTo(1L)
    assertThat(monthRow.months[1].name).isEqualTo("November")
    assertThat(monthRow.year).isEqualTo(2022)
  }

  @Test
  fun `verify creation of row with crossing months of different weeks`() {
    val startDate = LocalDate.of(2022, 10, 27)
    val endDate = startDate.plusDays(6)

    val monthRow = cut.assemble(startDate, endDate, WORKDAY_CONFIGURATION)

    assertThat(monthRow).isNotNull
    assertThat(monthRow.months).isNotNull
    assertThat(monthRow.months).hasSize(2)
    assertThat(monthRow.months[0].length).isEqualTo(6L)
    assertThat(monthRow.months[0].name).isEqualTo("October")
    assertThat(monthRow.months[1].length).isEqualTo(8L)
    assertThat(monthRow.months[1].name).isEqualTo("November")
    assertThat(monthRow.year).isEqualTo(2022)
  }

  @Test
  fun `verify creation of row with crossing years of the same week`() {
    val startDate = LocalDate.of(2022, 12, 29)
    val endDate = startDate.plusDays(1)

    val monthRow = cut.assemble(startDate, endDate, WORKDAY_CONFIGURATION)

    assertThat(monthRow).isNotNull
    assertThat(monthRow.months).isNotNull
    assertThat(monthRow.months).hasSize(2)
    assertThat(monthRow.months[0].length).isEqualTo(4L)
    assertThat(monthRow.months[0].name).isEqualTo("December")
    assertThat(monthRow.months[1].length).isEqualTo(3L)
    assertThat(monthRow.months[1].name).isEqualTo("January")
    assertThat(monthRow.year).isEqualTo(2022)
  }

  @Test
  fun `verify creation of row with crossing years of different weeks`() {
    val startDate = LocalDate.of(2019, 12, 29)
    val endDate = startDate.plusDays(6)

    val monthRow = cut.assemble(startDate, endDate, WORKDAY_CONFIGURATION)

    assertThat(monthRow).isNotNull
    assertThat(monthRow.months).isNotNull
    assertThat(monthRow.months).hasSize(2)
    assertThat(monthRow.months[0].length).isEqualTo(7L)
    assertThat(monthRow.months[0].name).isEqualTo("December")
    assertThat(monthRow.months[1].length).isEqualTo(7L)
    assertThat(monthRow.months[1].name).isEqualTo("January")
    assertThat(monthRow.year).isEqualTo(2019)
  }

  @Test
  fun `verify creation of row with crossing multiple months`() {
    val startDate = LocalDate.of(2022, 9, 8)
    val endDate = LocalDate.of(2022, 11, 29)

    val monthRow = cut.assemble(startDate, endDate, WORKDAY_CONFIGURATION)

    assertThat(monthRow).isNotNull
    assertThat(monthRow.months).isNotNull
    assertThat(monthRow.months).hasSize(3)
    assertThat(monthRow.months[0].length).isEqualTo(24L)
    assertThat(monthRow.months[0].name).isEqualTo("September")
    assertThat(monthRow.months[1].length).isEqualTo(31L)
    assertThat(monthRow.months[1].name).isEqualTo("October")
    assertThat(monthRow.months[2].length).isEqualTo(29L)
    assertThat(monthRow.months[2].name).isEqualTo("November")
    assertThat(monthRow.year).isEqualTo(2022)
  }

  companion object {
    private val WORKDAY_CONFIGURATION = buildWorkdayConfiguration()

    @JvmStatic @BeforeAll fun init(): Unit = setLocale(UK)
  }
}
