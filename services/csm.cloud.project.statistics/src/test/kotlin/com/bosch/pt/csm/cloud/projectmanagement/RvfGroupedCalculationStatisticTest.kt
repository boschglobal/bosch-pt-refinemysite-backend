/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitTaskScheduleWithDayCardsG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.NamedEnumReference
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.BAD_WEATHER
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.MANPOWER_SHORTAGE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class RvfGroupedCalculationStatisticTest : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var controller: StatisticsController

  @BeforeEach
  fun init() {
    initSecurityContext()
  }

  @Test
  fun `with one week and one task`() {
    val duration = 1L

    val dayCards =
        mapOf(
            startDate to Pair(DONE, null),
            startDate.plusDays(2) to Pair(NOTDONE, MANPOWER_SHORTAGE),
            startDate.plusDays(3) to Pair(NOTDONE, MANPOWER_SHORTAGE),
            startDate.plusDays(4) to Pair(NOTDONE, BAD_WEATHER))
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

    val statistics = controller.calculateRfv(project, startDate, duration, true)

    assertThat(statistics.items)
        .hasSize(1)
        .extracting("start", "end")
        .containsOnly(tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7)))

    val item =
        statistics.items.find {
          equalIdentifier(it.company, companyA) && equalIdentifier(it.projectCraft, craftA)
        }!!

    assertThat(item.totals.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 1L),
            tuple(
                NamedEnumReference(
                    DayCardReasonVarianceEnum.MANPOWER_SHORTAGE, WORKER_SHORTAGE_TRANSLATION),
                2L))

    assertThat(item.series)
        .hasSize(1)
        .extracting("start", "end")
        .containsExactly(tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7)))

    assertThat(item.series[0].metrics.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 1L),
            tuple(
                NamedEnumReference(
                    DayCardReasonVarianceEnum.MANPOWER_SHORTAGE, WORKER_SHORTAGE_TRANSLATION),
                2L))
  }

  @Test
  fun `with two weeks and two tasks`() {
    val duration = 2L

    val dayCards =
        mapOf(
            startDate to Pair(DONE, null),
            startDate.plusDays(2) to Pair(NOTDONE, MANPOWER_SHORTAGE),
            startDate.plusDays(3) to Pair(NOTDONE, MANPOWER_SHORTAGE),
            startDate.plusDays(8) to Pair(NOTDONE, BAD_WEATHER),
            startDate.plusDays(10) to Pair(NOTDONE, BAD_WEATHER),
            startDate.plusDays(12) to Pair(NOTDONE, BAD_WEATHER))

    val anotherDayCards =
        mapOf(
            startDate to Pair(DONE, null),
            startDate.plusDays(3) to Pair(NOTDONE, MANPOWER_SHORTAGE),
            startDate.plusDays(5) to Pair(NOTDONE, BAD_WEATHER),
            startDate.plusDays(6) to Pair(NOTDONE, BAD_WEATHER),
            startDate.plusDays(10) to Pair(DONE, null),
            startDate.plusDays(12) to Pair(NOTDONE, BAD_WEATHER))

    eventStreamGenerator.submitTask(asReference = "another-task") {
      it.craft = craft1
      it.assignee = participantFmA
    }

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("another-task", anotherDayCards)

    val statistics = controller.calculateRfv(project, startDate, duration, true)

    assertThat(statistics.items)
        .hasSize(1)
        .extracting("start", "end")
        .containsOnly(tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 14)))

    val item =
        statistics.items.find {
          equalIdentifier(it.company, companyA) && equalIdentifier(it.projectCraft, craftA)
        }!!

    assertThat(item.totals.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 6L),
            tuple(
                NamedEnumReference(
                    DayCardReasonVarianceEnum.MANPOWER_SHORTAGE, WORKER_SHORTAGE_TRANSLATION),
                3L))

    assertThat(item.series)
        .hasSize(2)
        .extracting("start", "end")
        .containsExactly(
            tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7)), // 1st week
            tuple(LocalDate.of(2018, 1, 8), LocalDate.of(2018, 1, 14))) // 2nd week

    assertThat(item.series[0].metrics.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 2L),
            tuple(
                NamedEnumReference(
                    DayCardReasonVarianceEnum.MANPOWER_SHORTAGE, WORKER_SHORTAGE_TRANSLATION),
                3L))

    assertThat(item.series[1].metrics.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 4L))
  }

  @Test
  fun `with one week and two crafts`() {
    val duration = 1L

    val dayCards =
        mapOf(
            startDate.plusDays(2) to Pair(NOTDONE, BAD_WEATHER),
            startDate.plusDays(3) to Pair(NOTDONE, BAD_WEATHER))

    val anotherDayCards =
        mapOf(
            startDate.plusDays(2) to Pair(NOTDONE, MANPOWER_SHORTAGE),
            startDate.plusDays(3) to Pair(NOTDONE, MANPOWER_SHORTAGE))

    eventStreamGenerator.submitTask(asReference = "another-task") {
      it.craft = craft2
      it.assignee = participantFmA
    }

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("another-task", anotherDayCards)

    val statistics = controller.calculateRfv(project, startDate, duration, true)

    assertThat(statistics.items)
        .hasSize(2)
        .extracting("start", "end")
        .containsOnly(tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7)))

    val itemCraftA =
        statistics.items.find {
          equalIdentifier(it.company, companyA) && equalIdentifier(it.projectCraft, craftA)
        }!!
    val itemCraftB =
        statistics.items.find {
          equalIdentifier(it.company, companyA) && equalIdentifier(it.projectCraft, craftB)
        }!!

    assertThat(itemCraftA.totals.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 2L))

    assertThat(itemCraftA.series)
        .hasSize(1)
        .extracting("start", "end")
        .containsExactly(tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7)))

    assertThat(itemCraftA.series[0].metrics.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(DayCardReasonVarianceEnum.BAD_WEATHER, WEATHER_TRANSLATION), 2L))

    assertThat(itemCraftB.totals.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(
                    DayCardReasonVarianceEnum.MANPOWER_SHORTAGE, WORKER_SHORTAGE_TRANSLATION),
                2L))

    assertThat(itemCraftB.series)
        .hasSize(1)
        .extracting("start", "end")
        .containsExactly(tuple(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7)))

    assertThat(itemCraftB.series[0].metrics.rfv)
        .extracting("reason", "value")
        .containsOnly(
            tuple(
                NamedEnumReference(
                    DayCardReasonVarianceEnum.MANPOWER_SHORTAGE, WORKER_SHORTAGE_TRANSLATION),
                2L))
  }

  private fun equalIdentifier(resource: ResourceReference?, identifier: AggregateIdentifierAvro) =
      resource != null && resource.identifier.toString() == identifier.identifier

  companion object {
    private const val WEATHER_TRANSLATION = "Weather"
    private const val WORKER_SHORTAGE_TRANSLATION = "Worker shortage"
  }
}
