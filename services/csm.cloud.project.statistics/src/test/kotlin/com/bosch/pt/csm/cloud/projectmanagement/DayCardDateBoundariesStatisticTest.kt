/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInDifferentWeeks
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInSameWeek
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitTaskScheduleWithDayCardsG2
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class DayCardDateBoundariesStatisticTest : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var controller: StatisticsController

  @BeforeEach
  fun init() {
    initSecurityContext()
  }

  @Test
  fun `in same week`() {
    eventStreamGenerator.submitDayCardsInSameWeek(startDate)

    val statistics = controller.calculatePpc(project, startDate, 1).items[0]

    assertThat(statistics.start).isEqualTo(startDate)
    assertThat(statistics.end).isEqualTo(startDate.plusDays(6))
    assertThat(statistics.series).hasSize(1)
    assertThat(statistics.series[0].start).isEqualTo(startDate)
    assertThat(statistics.series[0].end).isEqualTo(startDate.plusDays(6))
  }

  @Test
  fun `in adjacent weeks`() {
    val startDateWeekOne = LocalDate.ofYearDay(2018, 1)
    eventStreamGenerator.submitDayCardsInDifferentWeeks(startDateWeekOne, 1)

    val statistics = controller.calculatePpc(project, startDateWeekOne, 2).items[0]

    assertThat(statistics.start).isEqualTo(startDateWeekOne)
    assertThat(statistics.end).isEqualTo(startDateWeekOne.plusWeeks(1).plusDays(6))
    assertThat(statistics.series).hasSize(2)
    assertThat(statistics.series[0].start).isEqualTo(startDateWeekOne)
    assertThat(statistics.series[0].end).isEqualTo(startDateWeekOne.plusDays(6))
    assertThat(statistics.series[1].start).isEqualTo(startDateWeekOne.plusWeeks(1))
    assertThat(statistics.series[1].end).isEqualTo(startDateWeekOne.plusWeeks(1).plusDays(6))
  }

  @Test
  fun `in non-adjacent weeks`() {
    val startDateWeekOne = LocalDate.ofYearDay(2018, 1)
    eventStreamGenerator.submitDayCardsInDifferentWeeks(startDateWeekOne, 2)

    val statistics = controller.calculatePpc(project, startDateWeekOne, 3).items[0]

    assertThat(statistics.start).isEqualTo(startDateWeekOne)
    assertThat(statistics.end).isEqualTo(startDateWeekOne.plusWeeks(2).plusDays(6))
    assertThat(statistics.series).hasSize(3)
    assertThat(statistics.series[0].start).isEqualTo(startDateWeekOne)
    assertThat(statistics.series[0].end).isEqualTo(startDateWeekOne.plusDays(6))
    assertThat(statistics.series[2].start).isEqualTo(startDateWeekOne.plusWeeks(2))
    assertThat(statistics.series[2].end).isEqualTo(startDateWeekOne.plusWeeks(2).plusDays(6))
  }

  @Test
  fun `in non-adjacent weeks but start day is friday`() {
    val startDateWeekOne = LocalDate.ofYearDay(2018, 5) // friday 5. january
    val startDateWeekTwo = LocalDate.ofYearDay(2018, 12) // friday 12. january

    val dayCards =
        mapOf(
            startDateWeekOne to Pair(DONE, null), // friday 5. january
            startDateWeekOne.plusDays(7) to Pair(OPEN, null), // friday 12. january

            startDateWeekTwo.plusDays(1) to Pair(DONE, null), // saturday 13. january
            startDateWeekTwo.plusDays(2) to Pair(DONE, null), // sunday 14. january
            startDateWeekTwo.plusDays(3) to Pair(DONE, null)) // monday 15. january

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

    val statistics = controller.calculateRfv(project, startDateWeekOne, 4).items[0]

    assertThat(statistics.start)
      .isEqualTo(startDateWeekOne) // friday 5. january
    assertThat(statistics.end)
      .isEqualTo(LocalDate.of(2018, 2, 1)) // thursday 1. february
    assertThat(statistics.series).hasSize(4)
    assertThat(statistics.series[0].start)
      .isEqualTo(startDateWeekOne) // friday 5. january
    assertThat(statistics.series[0].end)
        .isEqualTo(LocalDate.of(2018, 1, 11)) // thursday 11. january
    assertThat(statistics.series[1].start)
        .isEqualTo(LocalDate.of(2018, 1, 12)) // friday 12. january
    assertThat(statistics.series[1].end)
        .isEqualTo(LocalDate.of(2018, 1, 18)) // thursday 18. january
    assertThat(statistics.series[2].start)
        .isEqualTo(LocalDate.of(2018, 1, 19)) // friday 19. january
    assertThat(statistics.series[2].end)
        .isEqualTo(LocalDate.of(2018, 1, 25)) // thursday 25. january
    assertThat(statistics.series[3].start)
        .isEqualTo(LocalDate.of(2018, 1, 26)) // friday 26. january
    assertThat(statistics.series[3].end)
      .isEqualTo(LocalDate.of(2018, 2, 1)) // thursday 1. february
  }

  @Test
  fun `in different years`() {
    eventStreamGenerator.submitDayCardsInDifferentWeeks(startDate, 53)

    val statistics = controller.calculatePpc(project, startDate, 54).items[0]

    assertThat(statistics.start).isEqualTo(startDate)
    assertThat(statistics.end).isEqualTo(startDate.plusWeeks(53).plusDays(6))
    assertThat(statistics.series).hasSize(54)
    assertThat(statistics.series[0].start).isEqualTo(startDate)
    assertThat(statistics.series[0].end).isEqualTo(startDate.plusDays(6))
    assertThat(statistics.series[53].start).isEqualTo(startDate.plusWeeks(53))
    assertThat(statistics.series[53].end).isEqualTo(startDate.plusWeeks(53).plusDays(6))
  }
}
