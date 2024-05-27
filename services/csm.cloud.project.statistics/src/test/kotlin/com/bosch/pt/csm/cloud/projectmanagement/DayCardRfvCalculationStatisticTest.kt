/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsForDifferentTasks
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInDifferentWeeks
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInSameWeek
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsWithState
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitTaskScheduleWithDayCardsG2
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.NamedEnumReference
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class DayCardRfvCalculationStatisticTest : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var controller: StatisticsController

  @BeforeEach
  fun init() {
    initSecurityContext()
  }

  @Test
  fun `in same week`() {
    eventStreamGenerator.submitDayCardsInSameWeek(startDate)

    val statistics = controller.calculateRfv(project, startDate, 1).items[0]

    assertThat(statistics.totals.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series).hasSize(1)

    assertThat(statistics.series[0].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))
  }

  @Test
  fun `in same week with different tasks`() {
    eventStreamGenerator.submitDayCardsForDifferentTasks(startDate)

    val statistics = controller.calculateRfv(project, startDate, 1).items[0]

    assertThat(statistics.totals.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series).hasSize(1)

    assertThat(statistics.series[0].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))
  }

  @Test
  fun `in adjacent weeks`() {
    eventStreamGenerator.submitDayCardsInDifferentWeeks(startDate, 1)

    val statistics = controller.calculateRfv(project, startDate, 2).items[0]

    assertThat(statistics.totals.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                2L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(2L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series).hasSize(2)

    assertThat(statistics.series[0].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series[1].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))
  }

  @Test
  fun `in non-adjacent weeks`() {
    eventStreamGenerator.submitDayCardsInDifferentWeeks(startDate, 2)

    val statistics = controller.calculateRfv(project, startDate, 4).items[0]

    assertThat(statistics.totals.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                2L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(2L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series).hasSize(4)

    assertThat(statistics.series[0].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series[2].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(
                1L, NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION)),
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))
  }

  @Test
  fun `start day of the week is friday in non-adjacent weeks`() {
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

    assertThat(statistics.totals.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series).hasSize(4)

    assertThat(statistics.series[0].metrics.rfv).isNull()

    assertThat(statistics.series[1].metrics.rfv)
        .extracting("value", "reason")
        .containsExactlyInAnyOrder(
            tuple(1L, NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION)))

    assertThat(statistics.series[2].metrics.rfv).isNull()
  }

  @Test
  fun `with state done`() {
    eventStreamGenerator.submitDayCardsWithState(startDate, DONE)

    val statistics = controller.calculateRfv(project, startDate, 1).items.first()

    assertThat(statistics.totals.rfv!!).isEmpty()
    assertThat(statistics.series).hasSize(1)
    assertThat(statistics.series[0].metrics.rfv).isNull()
  }

  @Test
  fun `with state approved`() {
    eventStreamGenerator.submitDayCardsWithState(startDate, APPROVED)

    val statistics = controller.calculateRfv(project, startDate, 1).items[0]

    assertThat(statistics.totals.rfv!!).isEmpty()
    assertThat(statistics.series).hasSize(1)
    assertThat(statistics.series[0].metrics.rfv).isNull()
  }

  @Test
  fun `with state open`() {
    eventStreamGenerator.submitDayCardsWithState(startDate, OPEN)

    val statistics = controller.calculateRfv(project, startDate, 1).items[0]

    assertThat(statistics.totals.rfv!!.size).isEqualTo(1)
    assertThat(statistics.totals.rfv!![0].reason)
        .isEqualTo(NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION))
    assertThat(statistics.totals.rfv!![0].value).isEqualTo(3)
    assertThat(statistics.series).hasSize(1)
    assertThat(statistics.series[0].metrics.rfv!![0].reason)
        .isEqualTo(NamedEnumReference(DayCardReasonVarianceEnum.OPEN, OPEN_TRANSLATION))
  }

  @Test
  fun `with state not done`() {
    eventStreamGenerator.submitDayCardsWithState(startDate, NOTDONE)

    val statistics = controller.calculateRfv(project, startDate, 1).items[0]

    assertThat(statistics.totals.rfv!![0].value).isEqualTo(3)
    assertThat(statistics.totals.rfv!![0].reason)
        .isEqualTo(NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION))
    assertThat(statistics.series).hasSize(1)
    assertThat(statistics.series[0].metrics.rfv!![0].value).isEqualTo(3)
    assertThat(statistics.series[0].metrics.rfv!![0].reason)
        .isEqualTo(NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION))
  }

  @Test
  fun `with no dayCards at all`() {
    val startDate = LocalDate.now()
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-1", emptyMap(), startDate, startDate.plusWeeks(3))

    val statistics = controller.calculateRfv(project, LocalDate.now(), 2).items[0]

    assertThat(statistics.series.size).isEqualTo(2)
    assertThat(statistics.totals.rfv!!).hasSize(0)
    assertThat(statistics.series[0].metrics.rfv).isNull()
    assertThat(statistics.series[1].metrics.rfv).isNull()
  }

  companion object {
    private const val WEATHER_TRANSLATION = "Weather"
    private const val OPEN_TRANSLATION = "Open day cards"
  }
}
