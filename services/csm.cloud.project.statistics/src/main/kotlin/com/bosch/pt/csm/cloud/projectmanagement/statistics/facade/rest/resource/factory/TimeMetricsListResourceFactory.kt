/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.TimeMetricsResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.TimeFrame
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class TimeMetricsListResourceFactory(
    private val timeMetricsResourceFactory: TimeMetricsResourceFactory
) {

  fun build(
      startDate: LocalDate,
      duration: Long,
      ppcTimeMetrics: Map<TimeFrame, Long?>,
      rfvTimeMetrics: Map<TimeFrame, Map<DayCardReasonVarianceEnum, Long>>,
      translatedRfvs: Map<DayCardReasonVarianceEnum, String>
  ): List<TimeMetricsResource> {

    val resources: MutableList<TimeMetricsResource> = ArrayList()

    val ppcTimeMetricsByWeek = ppcTimeMetrics.mapKeys { it.key.week }
    val rfvTimeMetricsByWeek = rfvTimeMetrics.mapKeys { it.key.week }

    for (week in 0 until duration) {
      val weekPpc = ppcTimeMetricsByWeek[week]
      val weekRfv = rfvTimeMetricsByWeek[week]

      resources.add(
          timeMetricsResourceFactory.build(
              startDate.plusWeeks(week),
              startDate.plusWeeks(week + 1L).minusDays(1L),
              weekPpc,
              weekRfv,
              translatedRfvs))
    }
    return resources
  }
}
