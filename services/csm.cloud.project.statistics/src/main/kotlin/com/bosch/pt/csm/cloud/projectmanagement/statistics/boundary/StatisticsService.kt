/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountGroupedEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountGroupedEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.DayCardStatisticsRepository
import datadog.trace.api.Trace
import java.time.LocalDate
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
class StatisticsService(private val dayCardStatisticsRepository: DayCardStatisticsRepository) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@statisticsAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  fun calculatePpc(
      projectIdentifier: UUID,
      startDate: LocalDate,
      durationWeeks: Long
  ): List<DayCardCountEntry> {
    Assert.isTrue(durationWeeks > 0, DURATION_LARGER_ZERO)
    return dayCardStatisticsRepository.getDayCardCountPerStatusAndWeekWithinDates(
        projectIdentifier, startDate, startDate, startDate, determineEndDate(startDate, durationWeeks))
  }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@statisticsAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  fun calculatePpcByCompanyAndCraft(
      projectIdentifier: UUID,
      startDate: LocalDate,
      durationWeeks: Long
  ): List<DayCardCountGroupedEntry> {
    Assert.isTrue(durationWeeks > 0, DURATION_LARGER_ZERO)
    return dayCardStatisticsRepository.getDayCardCountPerStatusAndWeekWithinDatesGrouped(
        projectIdentifier, startDate, determineEndDate(startDate, durationWeeks))
  }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@statisticsAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  fun calculateRfv(
      projectIdentifier: UUID,
      startDate: LocalDate,
      durationWeeks: Long
  ): List<DayCardReasonCountEntry> {
    Assert.isTrue(durationWeeks > 0, DURATION_LARGER_ZERO)
    return dayCardStatisticsRepository.getDayCardCountPerReasonAndWeekWithinDates(
        projectIdentifier, startDate, determineEndDate(startDate, durationWeeks))
  }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@statisticsAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  fun calculateRfvByCompanyAndCraft(
      projectIdentifier: UUID,
      startDate: LocalDate,
      durationWeeks: Long
  ): List<DayCardReasonCountGroupedEntry> {
    Assert.isTrue(durationWeeks > 0, DURATION_LARGER_ZERO)
    return dayCardStatisticsRepository.getDayCardCountPerReasonAndWeekWithinDatesGrouped(
        projectIdentifier, startDate, determineEndDate(startDate, durationWeeks))
  }

  @Trace
  fun determineEndDate(startDate: LocalDate, durationWeeks: Long): LocalDate =
      startDate.plusWeeks(durationWeeks).minusDays(1)

  companion object {
    private const val DURATION_LARGER_ZERO = "duration must be larger than 0"
  }
}
