/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.MetricsResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.NamedEnumReference
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.RfvMetricsResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import org.springframework.stereotype.Component

@Component
class MetricsResourceFactory {

  fun build(
      ppc: Long?,
      rfv: Map<DayCardReasonVarianceEnum, Long>?,
      translatedRfvs: Map<DayCardReasonVarianceEnum, String>
  ) =
      MetricsResource(
          ppc,
          rfv?.map {
            RfvMetricsResource(NamedEnumReference(it.key, translatedRfvs[it.key]!!), it.value)
          })
}
