/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.time.LocalDate

class StatisticsResource(
    var start: LocalDate,
    var end: LocalDate,
    var totals: MetricsResource,
    var series: List<TimeMetricsResource>,
    var company: ResourceReference?,
    var projectCraft: ResourceReference?
)
