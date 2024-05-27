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
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class TimeMetricsResourceFactory(private val metricsResourceFactory: MetricsResourceFactory) {

  fun build(
      startDate: LocalDate,
      endDate: LocalDate,
      weekPpc: Long?,
      weekRfv: Map<DayCardReasonVarianceEnum, Long>?,
      translatedRfvs: Map<DayCardReasonVarianceEnum, String>
  ) =
      TimeMetricsResource(
          startDate, endDate, metricsResourceFactory.build(weekPpc, weekRfv, translatedRfvs))
}
