/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.MetricsResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.StatisticsResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.TimeMetricsResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.NamedObject
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class StatisticsResourceFactory {

  fun build(
      startDate: LocalDate,
      endDate: LocalDate,
      timeMetricsSeries: List<TimeMetricsResource>,
      totals: MetricsResource,
      company: NamedObject? = null,
      projectCraft: NamedObject? = null
  ) =
      StatisticsResource(
          startDate,
          endDate,
          totals,
          timeMetricsSeries,
          company?.let { ResourceReference.from(it) },
          projectCraft?.let { ResourceReference.from(it) })
}
