/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.StatisticsService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator.PpcByCompanyAndCraftCalculator
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator.PpcCalculator
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator.RfvByCompanyAndCraftCalculator
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.calculator.RfvCalculator
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.StatisticsListResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory.StatisticsListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.toRfv
import java.time.LocalDate
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.util.Assert
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
@Validated
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val statisticsListResourceFactory: StatisticsListResourceFactory
) {

  @GetMapping(PROJECT_METRICS_ENDPOINT)
  fun getMetricsForProjects(
      @PathVariable("projectId") projectId: UUID,
      @RequestParam(name = "grouped", required = false, defaultValue = "false") grouped: Boolean,
      @RequestParam(name = "startDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      startDate: LocalDate,
      @RequestParam(name = "duration") duration: Long,
      @RequestParam(name = "type") type: List<String>
  ): StatisticsListResource {
    Assert.isTrue(duration > 0, "Duration is too small")
    Assert.isTrue(type.isNotEmpty(), "Type required")

    return when (grouped) {
      true -> getMetricsForProjectsGrouped(projectId, startDate, duration, type)
      else -> getMetricsForProjectsUngrouped(projectId, startDate, duration, type)
    }
  }

  private fun getMetricsForProjectsUngrouped(
      projectId: UUID,
      startDate: LocalDate,
      duration: Long,
      type: List<String>
  ): StatisticsListResource {

    val dayCardCounts = statisticsService.calculatePpc(projectId, startDate, duration)

    val ppc = if (type.contains(PPC_TYPE)) PpcCalculator(dayCardCounts) else null

    val rfv =
        if (type.contains(RFV_TYPE))
            RfvCalculator(
                statisticsService.calculateRfv(projectId, startDate, duration) +
                    dayCardCounts.toRfv())
        else null

    return statisticsListResourceFactory.buildUngrouped(
        projectId,
        startDate,
        statisticsService.determineEndDate(startDate, duration),
        duration,
        ppc?.total(),
        ppc?.perWeek(startDate),
        rfv?.total(),
        rfv?.perWeek(startDate))
  }

  private fun getMetricsForProjectsGrouped(
      projectId: UUID,
      startDate: LocalDate,
      duration: Long,
      type: List<String>
  ): StatisticsListResource {

    val dayCardCountGroupedEntries =
        statisticsService.calculatePpcByCompanyAndCraft(projectId, startDate, duration)

    val ppc =
        if (type.contains(PPC_TYPE)) PpcByCompanyAndCraftCalculator(dayCardCountGroupedEntries)
        else null

    val rfv =
        if (type.contains(RFV_TYPE))
            RfvByCompanyAndCraftCalculator(
                statisticsService.calculateRfvByCompanyAndCraft(projectId, startDate, duration) +
                    dayCardCountGroupedEntries.toRfv())
        else null

    return statisticsListResourceFactory.buildGrouped(
        projectId,
        startDate,
        statisticsService.determineEndDate(startDate, duration),
        duration,
        ppc?.total() ?: emptyMap(),
        ppc?.perWeek(startDate) ?: emptyMap(),
        rfv?.total() ?: emptyMap(),
        rfv?.perWeek(startDate) ?: emptyMap())
  }

  companion object {
    const val PROJECT_METRICS_ENDPOINT = "/projects/{projectId}/metrics"
    const val PPC_TYPE = "ppc"
    const val RFV_TYPE = "rfv"
  }
}
