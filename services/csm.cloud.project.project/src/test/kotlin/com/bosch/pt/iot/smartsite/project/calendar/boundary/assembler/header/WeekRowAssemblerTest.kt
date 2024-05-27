/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.header

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekDayCellType.RESTING_DAY
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekDayCellType.WORKING_DAY
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.header.WeekRow
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.END_DATE
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.START_DATE
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildWorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import java.util.Locale
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import java.util.Locale.UK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder.setLocale

@SmartSiteSpringBootTest
class WeekRowAssemblerTest {

  @Autowired private lateinit var cut: WeekRowAssembler

  @Test
  fun `verify creation of row with one week`() {
    setLocale(UK)
    var weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].name).isEqualTo("Week 41")

    setLocale(GERMANY)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].name).isEqualTo("KW 41")

    setLocale(Locale("es", "ES"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].name).isEqualTo("Semana 41")

    setLocale(FRANCE)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].name).isEqualTo("Semaine 41")

    setLocale(Locale("pt", "PT"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].name).isEqualTo("Semana 41")
  }

  @Test
  fun `verify creation of row with crossing weeks`() {
    val endDate = START_DATE.plusDays(7)

    setLocale(UK)
    var weekRow: WeekRow =
        cut.assemble(
            START_DATE,
            endDate,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(2)
    assertThat(weekRow.weeks[0].name).isEqualTo("Week 41")
    assertThat(weekRow.weeks[1].name).isEqualTo("Week 42")

    setLocale(GERMANY)
    weekRow =
        cut.assemble(
            START_DATE,
            endDate,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(2)
    assertThat(weekRow.weeks[0].name).isEqualTo("KW 41")
    assertThat(weekRow.weeks[1].name).isEqualTo("KW 42")

    setLocale(Locale("es", "ES"))
    weekRow =
        cut.assemble(
            START_DATE,
            endDate,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(2)
    assertThat(weekRow.weeks[0].name).isEqualTo("Semana 41")
    assertThat(weekRow.weeks[1].name).isEqualTo("Semana 42")

    setLocale(FRANCE)
    weekRow =
        cut.assemble(
            START_DATE,
            endDate,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(2)
    assertThat(weekRow.weeks[0].name).isEqualTo("Semaine 41")
    assertThat(weekRow.weeks[1].name).isEqualTo("Semaine 42")

    setLocale(Locale("pt", "PT"))
    weekRow =
        cut.assemble(
            START_DATE,
            endDate,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(2)
    assertThat(weekRow.weeks[0].name).isEqualTo("Semana 41")
    assertThat(weekRow.weeks[1].name).isEqualTo("Semana 42")
  }

  @Test
  fun `verify creation row start and end date of one week`() {
    setLocale(UK)
    var weekRow: WeekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].startDate).isEqualTo("9 Oct.")
    assertThat(weekRow.weeks[0].endDate).isEqualTo("15 Oct.")

    setLocale(GERMANY)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].startDate).isEqualTo("9. Okt.")
    assertThat(weekRow.weeks[0].endDate).isEqualTo("15. Okt.")

    setLocale(Locale("es", "ES"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].startDate).isEqualTo("9 oct")
    assertThat(weekRow.weeks[0].endDate).isEqualTo("15 oct")

    setLocale(FRANCE)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].startDate).isEqualTo("9 oct.")
    assertThat(weekRow.weeks[0].endDate).isEqualTo("15 oct.")

    setLocale(Locale("pt", "PT"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].startDate).isEqualTo("9 out.")
    assertThat(weekRow.weeks[0].endDate).isEqualTo("15 out.")
  }

  @Test
  fun `verify creation row of one expanded week, in case of existing day cards`() {
    setLocale(UK)
    var weekRow: WeekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = true,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("Wed. 9 Oct.")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("Thu. 10 Oct.")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("Fri. 11 Oct.")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("Sat. 12 Oct.")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("Sun. 13 Oct.")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("Mon. 14 Oct.")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("Tue. 15 Oct.")

    setLocale(GERMANY)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = true,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("Mi. 9 Okt.")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("Do. 10 Okt.")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("Fr. 11 Okt.")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("Sa. 12 Okt.")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("So. 13 Okt.")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("Mo. 14 Okt.")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("Di. 15 Okt.")

    setLocale(Locale("es", "ES"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = true,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("mié 9 oct.")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("jue 10 oct.")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("vie 11 oct.")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("sáb 12 oct.")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("dom 13 oct.")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("lun 14 oct.")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("mar 15 oct.")

    setLocale(FRANCE)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = true,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("mer. 9 oct.")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("jeu. 10 oct.")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("ven. 11 oct.")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("sam. 12 oct.")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("dim. 13 oct.")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("lun. 14 oct.")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("mar. 15 oct.")

    setLocale(Locale("pt", "PT"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = true,
            includeMilestones = false)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("quarta, 9 out.")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("quinta, 10 out.")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("sexta, 11 out.")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("sábado, 12 out.")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("domingo, 13 out.")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("segunda, 14 out.")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("terça, 15 out.")
  }

  @Test
  fun `verify creation row of one expanded week, in case of only existing milestones`() {
    setLocale(UK)
    var weekRow: WeekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("9")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("10")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("11")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("12")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("13")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("14")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("15")

    setLocale(GERMANY)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("9")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("10")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("11")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("12")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("13")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("14")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("15")

    setLocale(Locale("es", "ES"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("9")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("10")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("11")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("12")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("13")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("14")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("15")

    setLocale(FRANCE)
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("9")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("10")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("11")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("12")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("13")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("14")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("15")

    setLocale(Locale("pt", "PT"))
    weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = false,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].name).isEqualTo("9")
    assertThat(weekRow.weeks[0].days[1].name).isEqualTo("10")
    assertThat(weekRow.weeks[0].days[2].name).isEqualTo("11")
    assertThat(weekRow.weeks[0].days[3].name).isEqualTo("12")
    assertThat(weekRow.weeks[0].days[4].name).isEqualTo("13")
    assertThat(weekRow.weeks[0].days[5].name).isEqualTo("14")
    assertThat(weekRow.weeks[0].days[6].name).isEqualTo("15")
  }

  @Test
  fun `verify creation row days type of one week based on workingdays`() {
    val weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION,
            includeDayCards = true,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[1].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[2].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[3].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[4].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[5].type).isEqualTo(RESTING_DAY)
    assertThat(weekRow.weeks[0].days[6].type).isEqualTo(RESTING_DAY)
  }

  @Test
  fun `verify creation row days type of one week based on holidays`() {
    val weekRow =
        cut.assemble(
            START_DATE,
            END_DATE,
            WORKDAY_CONFIGURATION_WITH_HOLIDAYS,
            includeDayCards = true,
            includeMilestones = true)

    assertThat(weekRow).isNotNull
    assertThat(weekRow.weeks).isNotNull
    assertThat(weekRow.weeks).hasSize(1)
    assertThat(weekRow.weeks[0].days).hasSize(7)
    assertThat(weekRow.weeks[0].days[0].type).isEqualTo(RESTING_DAY)
    assertThat(weekRow.weeks[0].days[1].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[2].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[3].type).isEqualTo(RESTING_DAY)
    assertThat(weekRow.weeks[0].days[4].type).isEqualTo(WORKING_DAY)
    assertThat(weekRow.weeks[0].days[5].type).isEqualTo(RESTING_DAY)
    assertThat(weekRow.weeks[0].days[6].type).isEqualTo(RESTING_DAY)
  }

  companion object {
    private val WORKDAY_CONFIGURATION = buildWorkdayConfiguration()

    private val WORKDAY_CONFIGURATION_WITH_HOLIDAYS =
        buildWorkdayConfiguration() {
          it.holidays =
              mutableSetOf(
                  Holiday("First Day", START_DATE), Holiday("Fourth day", START_DATE.plusDays(3)))
        }
  }
}
