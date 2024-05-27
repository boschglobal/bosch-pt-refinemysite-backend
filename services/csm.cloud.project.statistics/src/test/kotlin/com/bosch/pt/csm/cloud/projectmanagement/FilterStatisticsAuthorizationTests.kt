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
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitTaskScheduleWithDayCardsG2
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.StatisticsListResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@SmartSiteSpringBootTest
class FilterStatisticsAuthorizationTests : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var controller: StatisticsController
  @Autowired private lateinit var participantMappingRepository: ParticipantMappingRepository

  @BeforeEach
  fun createDefaultTaskSchedules() {
    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        taskName = "task-1",
        dayCards =
            mapOf(
                startDate to Pair(DayCardStatusEnumAvro.DONE, null),
                startDate.plusDays(2) to Pair(DayCardStatusEnumAvro.NOTDONE, null)))

    eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
        taskName = "task-2",
        dayCards =
            mapOf(
                startDate to Pair(DayCardStatusEnumAvro.DONE, null),
                startDate.plusDays(1) to Pair(DayCardStatusEnumAvro.APPROVED, null),
                startDate.plusDays(2) to Pair(DayCardStatusEnumAvro.NOTDONE, null)))
  }

  @Nested
  inner class CompanyPpcCalculationAuthorizationTest {

    @Test
    fun `for Admin role`() {
      initSecurityContext(randomUUID(), true)

      val statistics = controller.calculatePpc(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA, companyB))
    }

    @Test
    fun `for CSM`() {
      val participantCsm =
          participantMappingRepository.findOneByParticipantIdentifier(participantCsmA.toUUID())
      initSecurityContext(participantCsm!!.userIdentifier, false)

      val statistics = controller.calculatePpc(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA, companyB))
    }

    @Test
    fun `for CR`() {
      val participantCr =
          participantMappingRepository.findOneByParticipantIdentifier(participantCrA.toUUID())
      initSecurityContext(participantCr!!.userIdentifier, false)

      val statistics = controller.calculatePpc(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA))
    }

    @Test
    fun `for FM`() {
      val participantFm =
          participantMappingRepository.findOneByParticipantIdentifier(participantFmA.toUUID())
      initSecurityContext(participantFm!!.userIdentifier, false)

      val statistics = controller.calculatePpc(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA))
    }

    @Test
    fun `for Non-Participant`() {
      initSecurityContext(randomUUID(), false)

      assertThatThrownBy { controller.calculatePpc(project, startDate, 1, true) }
          .isInstanceOf(AccessDeniedException::class.java)
    }
  }

  @Nested
  inner class CompanyRfvCalculationAuthorizationTest {

    @Test
    fun `for Admin role`() {
      initSecurityContext(randomUUID(), true)

      val statistics = controller.calculateRfv(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA, companyB))
    }

    @Test
    fun `for CSM`() {
      val participantCsm =
          participantMappingRepository.findOneByParticipantIdentifier(participantCsmA.toUUID())
      initSecurityContext(participantCsm!!.userIdentifier, false)

      val statistics = controller.calculateRfv(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA, companyB))
    }

    @Test
    fun `for CR`() {
      val participantCr =
          participantMappingRepository.findOneByParticipantIdentifier(participantCrA.toUUID())
      initSecurityContext(participantCr!!.userIdentifier, false)

      val statistics = controller.calculateRfv(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA))
    }

    @Test
    fun `for FM`() {
      val participantFm =
          participantMappingRepository.findOneByParticipantIdentifier(participantFmA.toUUID())
      initSecurityContext(participantFm!!.userIdentifier, false)

      val statistics = controller.calculateRfv(project, startDate, 1, true)

      assertGroupedStatisticList(statistics, listOf(companyA))
    }

    @Test
    fun `for Non-Participant`() {
      initSecurityContext(randomUUID(), false)

      assertThatThrownBy { controller.calculateRfv(project, startDate, 1, true) }
          .isInstanceOf(AccessDeniedException::class.java)
    }
  }
  private fun assertGroupedStatisticList(
      statisticsListResource: StatisticsListResource,
      expectedList: List<AggregateIdentifierAvro>
  ) {

    assertThat(statisticsListResource.items).hasSize(expectedList.size)

    // apply sorting to not assume a specific order of the returned statistics
    val statistics =
        statisticsListResource.items.sortedWith(compareBy { it.company!!.identifier.toString() })

    expectedList.sortedWith(compareBy { it.identifier }).forEachIndexed { i, expected ->
      assertThat(statistics).hasSize(statistics.size)
      assertThat(statistics[i].company!!.identifier).isEqualTo(expected.toUUID())
      assertThat(statistics[i].series).hasSize(1)
    }
  }
}
