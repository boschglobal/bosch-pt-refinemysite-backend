/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TimeFrameTest {

  @Test
  fun newYear() {
    val startDate = LocalDate.parse("2018-12-31")
    val week0 = TimeFrame(0, startDate)

    assertThat(week0.startDate.dayOfWeek).isEqualTo(MONDAY)
    assertThat(week0.endDate.dayOfWeek).isEqualTo(SUNDAY)
    assertThat(week0.startDate).isEqualTo(LocalDate.parse("2018-12-31"))
    assertThat(week0.endDate).isEqualTo(LocalDate.parse("2019-01-06"))
  }

  @Test
  fun threeWeeks() {
    val startDate = LocalDate.parse("2019-02-20")
    assertThat(startDate.dayOfWeek).isEqualTo(WEDNESDAY)

    val week0 = TimeFrame(0, startDate)
    val week1 = TimeFrame(1, startDate)
    // skip week 2
    val week3 = TimeFrame(3, startDate)

    assertThat(week0.startDate.dayOfWeek).isEqualTo(WEDNESDAY)
    assertThat(week0.endDate.dayOfWeek).isEqualTo(TUESDAY)
    assertThat(week0.startDate).isEqualTo(LocalDate.parse("2019-02-20"))
    assertThat(week0.endDate).isEqualTo(LocalDate.parse("2019-02-26"))

    assertThat(week1.startDate.dayOfWeek).isEqualTo(WEDNESDAY)
    assertThat(week1.endDate.dayOfWeek).isEqualTo(TUESDAY)
    assertThat(week1.startDate).isEqualTo(LocalDate.parse("2019-02-27"))
    assertThat(week1.endDate).isEqualTo(LocalDate.parse("2019-03-05"))

    assertThat(week3.startDate.dayOfWeek).isEqualTo(WEDNESDAY)
    assertThat(week3.endDate.dayOfWeek).isEqualTo(TUESDAY)
    assertThat(week3.startDate).isEqualTo(LocalDate.parse("2019-03-13"))
    assertThat(week3.endDate).isEqualTo(LocalDate.parse("2019-03-19"))
  }

  @Test
  fun leapYear() {
    val startDateLeapYear = LocalDate.parse("2016-02-27")
    val startDateNonLeapYear = LocalDate.parse("2018-02-27")

    val week0LeapYear = TimeFrame(0, startDateLeapYear)
    assertThat(week0LeapYear.startDate).isEqualTo(LocalDate.parse("2016-02-27"))
    assertThat(week0LeapYear.endDate).isEqualTo(LocalDate.parse("2016-03-04")) // includes 29th Feb

    val week0NonLeapYear = TimeFrame(0, startDateNonLeapYear)
    assertThat(week0NonLeapYear.startDate).isEqualTo(LocalDate.parse("2018-02-27"))
    assertThat(week0NonLeapYear.endDate).isEqualTo(LocalDate.parse("2018-03-05")) // no 29th Feb
  }

  @Test
  fun multipleYears() {
    val startDate = LocalDate.parse("2001-01-01")

    // checked with an external Date calculator for correct values.
    val week1 = TimeFrame(1, startDate)
    assertThat(week1.startDate).isEqualTo(LocalDate.parse("2001-01-08"))
    assertThat(week1.endDate).isEqualTo(LocalDate.parse("2001-01-14"))

    val week10 = TimeFrame(10, startDate)
    assertThat(week10.startDate).isEqualTo(LocalDate.parse("2001-03-12"))
    assertThat(week10.endDate).isEqualTo(LocalDate.parse("2001-03-18"))

    val week100 = TimeFrame(100, startDate)
    assertThat(week100.startDate).isEqualTo(LocalDate.parse("2002-12-02"))
    assertThat(week100.endDate).isEqualTo(LocalDate.parse("2002-12-08"))

    val week1000 = TimeFrame(1000, startDate)
    assertThat(week1000.startDate).isEqualTo(LocalDate.parse("2020-03-02"))
    assertThat(week1000.endDate).isEqualTo(LocalDate.parse("2020-03-08"))

    val week2000 = TimeFrame(2000, startDate) // checks for timestamp Year 2038 problem
    assertThat(week2000.startDate).isEqualTo(LocalDate.parse("2039-05-02"))
    assertThat(week2000.endDate).isEqualTo(LocalDate.parse("2039-05-08"))
  }
}
