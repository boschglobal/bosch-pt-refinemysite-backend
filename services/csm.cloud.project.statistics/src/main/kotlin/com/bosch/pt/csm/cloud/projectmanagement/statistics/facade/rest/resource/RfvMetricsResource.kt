/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum

class RfvMetricsResource(
    var reason: NamedEnumReference<DayCardReasonVarianceEnum>,
    var value: Long
)
