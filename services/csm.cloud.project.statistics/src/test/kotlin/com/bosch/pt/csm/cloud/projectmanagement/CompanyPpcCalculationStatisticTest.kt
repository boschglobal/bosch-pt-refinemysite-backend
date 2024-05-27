/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInSameWeek
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitTaskScheduleWithDayCardsG2
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.StatisticsListResource
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class CompanyPpcCalculationStatisticTest : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var controller: StatisticsController

  @BeforeEach
  fun init() {
    initSecurityContext()
  }

  @Test
  fun `after a project has been deleted`() {
    val dayCards =
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null))
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

    var statistics = controller.calculatePpc(project, startDate, 1, true)
    assertGroupedStatisticsList(statistics, listOf(Triple(companyA, craftA, 50L)))

    statistics = controller.calculatePpc(project, startDate, 1, false)
    assertThat(statistics.items).hasSize(1)
    assertThat(statistics.items[0].totals.ppc).isEqualTo(50)
    assertThat(statistics.items[0].series).hasSize(1)
    assertThat(statistics.items[0].series[0].metrics.ppc).isEqualTo(50)

    // Delete data
    eventStreamGenerator.submitProject(eventType = ProjectEventEnumAvro.DELETED)

    statistics = controller.calculatePpc(project, startDate, 1, true)
    assertThat(statistics.items.size).isEqualTo(0)

    statistics = controller.calculatePpc(project, startDate, 1, false)
    assertThat(statistics.items).hasSize(1)
    assertThat(statistics.items[0].series).hasSize(1)
    assertThat(statistics.items[0].series[0].metrics.ppc).isNull()
  }

  @Test
  fun `for one company with one craft`() {
    val dayCards =
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null))
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

    val statistics = controller.calculatePpc(project, startDate, 1, true)

    assertGroupedStatisticsList(statistics, listOf(Triple(companyA, craftA, 50L)))
  }

  @Test
  fun `for one company with one craft and multiple tasks`() {
    val dayCards =
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null))

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

    eventStreamGenerator.submitTask(asReference = "task-2") {
      it.craft = craft1
      it.assignee = participantFmA
    }

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-2", mapOf(startDate.plusDays(3) to Pair(DONE, null)))

    val statistics = controller.calculatePpc(project, startDate, 1, true)

    assertGroupedStatisticsList(statistics, listOf(Triple(companyA, craftA, 66L)))
  }

  @Test
  fun `for one company with two crafts`() {
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-1",
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null)))

    eventStreamGenerator
        .submitProjectCraftG2(asReference = "another-craft") { it.name = "another-craft" }
        .submitTask(asReference = "another-task") {
          it.craft =
              eventStreamGenerator
                  .get<ProjectCraftAggregateG2Avro>("another-craft")!!
                  .aggregateIdentifier
          it.assignee = participantFmA
        }

    val anotherCraft =
        eventStreamGenerator.get<ProjectCraftAggregateG2Avro>("another-craft")!!.aggregateIdentifier
    craftIdentifierToNameMap[anotherCraft] = "another-craft"

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "another-task",
        mapOf(
            startDate.plusDays(3) to Pair(DONE, null),
            startDate.plusDays(4) to Pair(APPROVED, null),
            startDate.plusDays(5) to Pair(NOTDONE, null)))

    val statistics = controller.calculatePpc(project, startDate, 1, true)

    assertGroupedStatisticsList(
        statistics, listOf(Triple(companyA, craftA, 50L), Triple(companyA, anotherCraft, 66L)))
  }

  @Test
  fun `for two companies with the same craft`() {
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-1",
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null)))

    eventStreamGenerator
        .submitProjectCraftG2(asReference = "another-craft") { it.name = "another-craft" }
        .submitTask(asReference = "other-company-task") {
          it.craft = craft1
          it.assignee = participantFmB
        }

    val anotherCraft =
        eventStreamGenerator.get<ProjectCraftAggregateG2Avro>("another-craft")!!.aggregateIdentifier
    craftIdentifierToNameMap[anotherCraft] = "another-craft"

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "other-company-task",
        mapOf(
            startDate to Pair(DONE, null),
            startDate.plusDays(1) to Pair(APPROVED, null),
            startDate.plusDays(2) to Pair(NOTDONE, null)))

    val statistics = controller.calculatePpc(project, startDate, 1, true)

    assertGroupedStatisticsList(
        statistics, listOf(Triple(companyA, craftA, 50L), Triple(companyB, craftA, 66L)))
  }

  @Test
  fun `for two companies with different crafts`() {
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-1",
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null)))

    eventStreamGenerator
        .submitProjectCraftG2(asReference = "another-craft") { it.name = "another-craft" }
        .submitTask(asReference = "other-company-task") {
          it.craft =
              eventStreamGenerator
                  .get<ProjectCraftAggregateG2Avro>("another-craft")!!
                  .aggregateIdentifier
          it.assignee = participantFmB
        }

    val anotherCraft =
        eventStreamGenerator.get<ProjectCraftAggregateG2Avro>("another-craft")!!.aggregateIdentifier
    craftIdentifierToNameMap[anotherCraft] = "another-craft"

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "other-company-task",
        mapOf(
            startDate to Pair(DONE, null),
            startDate.plusDays(1) to Pair(APPROVED, null),
            startDate.plusDays(2) to Pair(NOTDONE, null)))

    val statistics = controller.calculatePpc(project, startDate, 1, true)

    assertGroupedStatisticsList(
        statistics,
        listOf(
            Triple(companyA, craftA, 50L),
            Triple(
                companyB,
                eventStreamGenerator
                    .get<ProjectCraftAggregateG2Avro>("another-craft")!!
                    .aggregateIdentifier,
                66L)))
  }

  @Test
  fun `for two companies with different crafts and one craft without company`() {
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-1",
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null)))

    eventStreamGenerator
        .submitProjectCraftG2(asReference = "another-craft") { it.name = "another-craft" }
        .submitTask(asReference = "other-company-task") {
          it.craft =
              eventStreamGenerator
                  .get<ProjectCraftAggregateG2Avro>("another-craft")!!
                  .aggregateIdentifier
          it.assignee = participantFmB
        }

    val anotherCraft =
        eventStreamGenerator.get<ProjectCraftAggregateG2Avro>("another-craft")!!.aggregateIdentifier
    craftIdentifierToNameMap[anotherCraft] = "another-craft"

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "other-company-task",
        mapOf(
            startDate to Pair(DONE, null),
            startDate.plusDays(1) to Pair(APPROVED, null),
            startDate.plusDays(2) to Pair(NOTDONE, null)))

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-3",
        mapOf(startDate to Pair(DONE, null), startDate.plusDays(2) to Pair(NOTDONE, null)))

    val statistics = controller.calculatePpc(project, startDate, 1, true)

    assertGroupedStatisticsList(
        statistics,
        listOf(
            Triple(companyA, craftA, 50L),
            Triple(
                companyB,
                eventStreamGenerator
                    .get<ProjectCraftAggregateG2Avro>("another-craft")!!
                    .aggregateIdentifier,
                66L),
            Triple(null, craftA, 50L)))
  }

  @Test
  fun `with no dayCards in the duration`() {
    eventStreamGenerator.submitDayCardsInSameWeek(startDate.plusWeeks(4))

    val statistics = controller.calculatePpc(project, LocalDate.now(), 2).items[0]

    assertThat(statistics.series.size).isEqualTo(2)
    assertThat(statistics.series[0].metrics.ppc).isNull()
    assertThat(statistics.series[1].metrics.ppc).isNull()
    assertThat(statistics.totals.ppc).isNull()
  }

  @Test
  fun `with no dayCards at all`() {
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        "task-1", emptyMap(), startDate, startDate.plusWeeks(3))

    val statistics = controller.calculatePpc(project, LocalDate.now(), 2).items[0]

    assertThat(statistics.series.size).isEqualTo(2)
    assertThat(statistics.series[0].metrics.ppc).isNull()
    assertThat(statistics.series[1].metrics.ppc).isNull()
    assertThat(statistics.totals.ppc).isNull()
  }

  private fun assertGroupedStatisticsList(
      statisticsList: StatisticsListResource,
      expectedList: List<Triple<AggregateIdentifierAvro?, AggregateIdentifierAvro, Long>>
  ) {

    // apply sorting to not assume a specific order of the returned statistics
    val statistics =
        statisticsList.items.sortedWith(
            compareBy(
                { it.company?.identifier.toString() }, { it.projectCraft!!.identifier.toString() }))

    expectedList
        .sortedWith(
            compareBy({ it.first?.identifier.toString() }, { it.second.identifier.toString() }))
        .forEachIndexed { i, expected ->
          assertThat(statistics).hasSize(statistics.size)
          assertThat(statistics[i].company?.identifier).isEqualTo(expected.first?.toUUID())
          assertThat(statistics[i].projectCraft!!.identifier).isEqualTo(expected.second.toUUID())
          assertThat(statistics[i].totals.ppc!!).isEqualTo(expected.third)
          assertThat(statistics[i].series).hasSize(1)
          assertThat(statistics[i].series[0].metrics.ppc!!).isEqualTo(expected.third)
        }
  }
}
